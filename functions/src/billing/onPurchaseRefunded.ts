import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * Google Play'den gelen iade bildirimlerini işler.
 * CLAUDE.md §18: Satın alma hakları istemciden değiştirilemez.
 *
 * Google Play Real-time Developer Notifications (RTDN) tetikler.
 * İade edilen "remove_ads_lifetime" satın alması → adsRemoved = false yapılır.
 *
 * NOT: Gerçek üretim ortamında bu fonksiyon Pub/Sub topic üzerinden
 * tetiklenmelidir. Şimdilik HTTP callable olarak test edilebilir.
 * Üretimde: functions.pubsub.topic('play-billing').onPublish(...) kullanın.
 */
export const onPurchaseRefunded = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    // Sadece admin veya doğrulanmış sistem çağrısı
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için yetkilendirme gereklidir."
      );
    }

    if (context.auth.token.role !== "admin") {
      throw new functions.https.HttpsError("permission-denied", "Bu işlem yalnızca yöneticiye açıktır.");
    }

    const { uid, purchaseToken, productId } = data;

    if (!uid || !purchaseToken || !productId) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "uid, purchaseToken ve productId zorunludur."
      );
    }

    // Satın alma kaydını bul
    const purchaseRef = db.collection("purchases").doc(uid);
    const purchaseSnap = await purchaseRef.get();

    if (!purchaseSnap.exists) {
      throw new functions.https.HttpsError(
        "not-found",
        "Satın alma kaydı bulunamadı."
      );
    }

    const purchaseData = purchaseSnap.data()!;

    // Yalnızca token eşleşiyorsa iade işlemi yap
    if (purchaseData.purchaseToken !== purchaseToken) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Satın alma token'ı eşleşmiyor."
      );
    }

    const batch = db.batch();

    // purchases/{uid} belgesini güncelle — iade edildi olarak işaretle
    batch.update(purchaseRef, {
      status: "refunded",
      refundedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Kullanıcı profilinden adsRemoved hakkını kaldır
    const userRef = db.collection("users").doc(uid);
    batch.update(userRef, {
      adsRemoved: false,
    });

    await batch.commit();

    // Kullanıcıya bildirim gönder
    const userSnap = await userRef.get();
    const fcmTokens: string[] = userSnap.data()?.fcmTokens || [];

    if (fcmTokens.length > 0) {
      try {
        await admin.messaging().sendEachForMulticast({
          tokens: fcmTokens,
          notification: {
            title: "Satın Alma İade Edildi",
            body: "Reklam kaldırma özelliğiniz iade edildi ve devre dışı bırakıldı.",
          },
          data: {
            type: "purchase_refunded",
            productId,
          },
        });
      } catch {
        // Bildirim başarısız olsa da işlemi iptal etme
      }
    }

    return { success: true, uid, productId };
  });