import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

const db = admin.firestore();

/**
 * FCM token'ı kullanıcının token listesinden kaldırır.
 * docs/19-notification-system.md
 *
 * Kullanıcı çıkış yaptığında veya bildirimleri kapattığında çağrılır.
 * updateFcmToken.ts ile simetrik çalışır.
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
    const existingTokens: string[] = userData.fcmTokens || [];

    // Token yoksa sessizce başarılı dön
    if (!existingTokens.includes(token)) {
      return { success: true };
    }

    const updatedTokens = existingTokens.filter((t) => t !== token);

    await userRef.update({
      fcmTokens: updatedTokens,
    });

    return { success: true };
  });