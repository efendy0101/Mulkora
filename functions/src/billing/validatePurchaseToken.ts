import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import { google } from "googleapis";
import * as crypto from "crypto";

/**
 * Google Play satın alma tokenını doğrulayan Cloud Function.
 *
 * CLAUDE.md §18:
 * - Satın alma tokenı yalnızca backend üzerinden doğrulanır.
 * - İstemci tarafından satın alma durumu değiştirilemez.
 * - Firestore `purchases/{uid}` belgesi bu Function tarafından güncellenir.
 *
 * Güvenlik:
 * - Kimlik doğrulaması zorunludur (context.auth.uid).
 * - productId sabit olarak sunucuda doğrulanır — istemciden kabul edilmez.
 * - Token kullanıcılar arası global tekil kontrolü: purchaseTokens/{tokenHash}.
 * - PLAY_SERVICE_ACCOUNT_JSON secret runWith ile bağlanır; yoksa prod'da hata fırlatılır.
 *
 * Google Play Developer API:
 * - googleapis paketi ile `purchases.products.get` çağrısı yapılır.
 * - Secret: `firebase functions:secrets:set PLAY_SERVICE_ACCOUNT_JSON`
 *   (Google Cloud Console → IAM → Service Account → JSON key içeriği)
 */

/** Sunucu tarafında sabit — istemciden kabul edilmez */
const REMOVE_ADS_PRODUCT_ID = "remove_ads_lifetime";
const PACKAGE_NAME = "com.mulkora.app";

export const validatePurchaseToken = functions
  .region("europe-west1")
  .runWith({ secrets: ["PLAY_SERVICE_ACCOUNT_JSON"] })
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlemi yapmak için giriş yapmanız gerekir."
      );
    }

    const uid = context.auth.uid;
    const { purchaseToken } = data as { purchaseToken?: string };

    if (!purchaseToken || typeof purchaseToken !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "purchaseToken gereklidir."
      );
    }

    // productId istemciden alınmaz — sunucu sabitini kullanır (güvenlik)
    const productId = REMOVE_ADS_PRODUCT_ID;

    const db = admin.firestore();
    const purchaseRef = db.collection("purchases").doc(uid);

    // Token hash — global tekil anahtar
    const tokenHash = crypto
      .createHash("sha256")
      .update(purchaseToken)
      .digest("hex");
    const globalTokenRef = db.collection("purchaseTokens").doc(tokenHash);

    try {
      // Google Play Developer API ile doğrulama
      const verificationResult = await verifyWithGooglePlay(
        PACKAGE_NAME,
        productId,
        purchaseToken
      );

      if (!verificationResult.isValid) {
        await purchaseRef.set(
          {
            status: "INVALID",
            purchaseToken,
            productId,
            uid,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            errorReason: verificationResult.reason ?? "unknown",
          },
          { merge: true }
        );
        throw new functions.https.HttpsError(
          "failed-precondition",
          `Satın alma doğrulanamadı: ${verificationResult.reason}`
        );
      }

      if (verificationResult.isRefunded) {
        await purchaseRef.set(
          {
            status: "REFUNDED",
            purchaseToken,
            productId,
            uid,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          },
          { merge: true }
        );

        await db.collection("users").doc(uid).update({
          adsRemoved: false,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        return { success: true, status: "REFUNDED" };
      }

      // Satın alma başarılı — transaction ile global tekil + kullanıcı hakları
      try {
        await db.runTransaction(async (tx) => {
          const globalSnap = await tx.get(globalTokenRef);

          if (globalSnap.exists) {
            const existing = globalSnap.data();
            if (existing?.uid === uid) {
              // Aynı kullanıcı — idempotent, işlem tekrar yapılmaz
              return;
            }
            // Farklı kullanıcı aynı token'ı kullanmaya çalışıyor
            throw new functions.https.HttpsError(
              "already-exists",
              "Bu satın alma tokenı zaten başka bir hesapta kullanılmış."
            );
          }

          const now = admin.firestore.FieldValue.serverTimestamp();

          // Global token kaydı — kullanıcıya kilitle
          tx.set(globalTokenRef, {
            uid,
            productId,
            usedAt: now,
          });

          // Kullanıcı satın alma belgesi
          tx.set(
            purchaseRef,
            {
              status: "PURCHASED",
              purchaseToken,
              productId,
              uid,
              purchasedAt: now,
              updatedAt: now,
              orderId: verificationResult.orderId ?? "",
            },
            { merge: true }
          );

          // Kullanıcı belgesine reklam kaldırma yetkisi
          tx.update(db.collection("users").doc(uid), {
            adsRemoved: true,
            updatedAt: now,
          });
        });
      } catch (txError) {
        if (txError instanceof functions.https.HttpsError) throw txError;
        throw txError;
      }

      functions.logger.info("Satın alma doğrulandı", {
        uid,
        productId,
        orderId: verificationResult.orderId,
      });

      return { success: true, status: "PURCHASED" };
    } catch (error) {
      if (error instanceof functions.https.HttpsError) throw error;
      functions.logger.error("validatePurchaseToken hatası", { uid, error });
      throw new functions.https.HttpsError(
        "internal",
        "Satın alma doğrulanırken bir hata oluştu."
      );
    }
  });

// ─── Google Play API ───────────────────────────────────────────────────────

interface VerificationResult {
  isValid: boolean;
  isRefunded: boolean;
  orderId?: string;
  reason?: string;
}

/**
 * Google Play Developer API ile satın almayı doğrular.
 *
 * Emulator ortamında (FUNCTIONS_EMULATOR=true) test tokenı döndürür.
 * Production'da PLAY_SERVICE_ACCOUNT_JSON yoksa hata fırlatır.
 *
 * purchaseState: 0=Purchased, 1=Canceled, 2=Pending
 * consumptionState: 0=Not consumed (tüketilmemiş ürün bekleniyor)
 */
async function verifyWithGooglePlay(
  packageName: string,
  productId: string,
  purchaseToken: string
): Promise<VerificationResult> {
  const serviceAccountJson = process.env.PLAY_SERVICE_ACCOUNT_JSON;

  // Emulator ortamında test moduna izin ver
  if (!serviceAccountJson) {
    if (process.env.FUNCTIONS_EMULATOR === "true") {
      functions.logger.warn(
        "PLAY_SERVICE_ACCOUNT_JSON yok — emulator test modu"
      );
      return {
        isValid: true,
        isRefunded: false,
        orderId: `DEV.${Date.now()}`,
      };
    }
    // Production'da secret yoksa işlemi reddet
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Ödeme doğrulama servisi yapılandırılmamış. Lütfen daha sonra tekrar deneyin."
    );
  }

  try {
    const serviceAccount = JSON.parse(serviceAccountJson);

    const auth = new google.auth.GoogleAuth({
      credentials: serviceAccount,
      scopes: ["https://www.googleapis.com/auth/androidpublisher"],
    });

    const androidPublisher = google.androidpublisher({ version: "v3", auth });

    const response = await androidPublisher.purchases.products.get({
      packageName,
      productId,
      token: purchaseToken,
    });

    const purchase = response.data;

    if (purchase.purchaseState !== 0) {
      return {
        isValid: false,
        isRefunded: false,
        reason: `Satın alma tamamlanmamış: purchaseState=${purchase.purchaseState}`,
      };
    }

    if (purchase.consumptionState !== 0) {
      return {
        isValid: false,
        isRefunded: true,
        reason: "Satın alma tüketilmiş veya iade edilmiş",
      };
    }

    return {
      isValid: true,
      isRefunded: false,
      orderId: purchase.orderId ?? undefined,
    };
  } catch (error: unknown) {
    const err = error as { code?: number; message?: string };
    functions.logger.error("Google Play API hatası", { error });

    if (err.code === 410) {
      return { isValid: false, isRefunded: true, reason: "Token süresi dolmuş veya iade edilmiş" };
    }
    if (err.code === 404) {
      return { isValid: false, isRefunded: false, reason: "Satın alma kaydı bulunamadı" };
    }

    return {
      isValid: false,
      isRefunded: false,
      reason: err.message ?? "Google Play API iletişim hatası",
    };
  }
}