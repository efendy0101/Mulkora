import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

const db = admin.firestore();

/**
 * FCM token'ı günceller.
 * docs/19-notification-system.md
 *
 * Android uygulaması başlatıldığında veya token yenilendiğinde çağrılır.
 * Token listesi maksimum 5 cihazı destekler (eski tokenlar temizlenir).
 */
export const updateFcmToken = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const { token } = data;

    if (!token || typeof token !== "string" || token.trim().length === 0) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir FCM token gereklidir."
      );
    }

    const userRef = db.collection("users").doc(uid);
    const userSnap = await userRef.get();

    if (!userSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Kullanıcı bulunamadı.");
    }

    const userData = userSnap.data()!;
    const existingTokens: string[] = userData.fcmTokens ?? [];

    // Token zaten varsa güncelleme gerekmez
    if (existingTokens.includes(token)) {
      return { success: true, updated: false };
    }

    // Maksimum 5 token tut (en eski tokenı çıkar)
    const MAX_TOKENS = 5;
    let updatedTokens = [...existingTokens, token];
    if (updatedTokens.length > MAX_TOKENS) {
      updatedTokens = updatedTokens.slice(updatedTokens.length - MAX_TOKENS);
    }

    await userRef.update({
      fcmTokens: updatedTokens,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`FCM token güncellendi: ${uid}`);

    return { success: true, updated: true };
  });

/**
 * FCM token'ı kaldırır (oturum kapatma sırasında çağrılır).
 */
export const removeFcmToken = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const { token } = data;

    if (!token || typeof token !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir FCM token gereklidir."
      );
    }

    await db.collection("users").doc(uid).update({
      fcmTokens: admin.firestore.FieldValue.arrayRemove(token),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`FCM token kaldırıldı: ${uid}`);

    return { success: true };
  });