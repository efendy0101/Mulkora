import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

/**
 * Kullanıcı hesabını siler.
 * CLAUDE.md Bölüm 2: Hesap Silme
 * docs/14-cloud-functions-architecture.md
 *
 * Yalnızca hesap sahibi kendi hesabını silebilir.
 * Firebase Auth kaydını ve Firestore verilerini temizler.
 * onUserDeleted trigger'ı otomatik olarak tetiklenir.
 */
export const deleteUserAccount = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;

    try {
      // Firebase Auth'dan kullanıcıyı sil
      // Bu işlem onUserDeleted trigger'ını tetikler
      await admin.auth().deleteUser(uid);

      functions.logger.info(`Kullanıcı hesabı silindi: ${uid}`);

      return { success: true };
    } catch (error) {
      functions.logger.error(`Hesap silinemedi: ${uid}`, error);
      throw new functions.https.HttpsError(
        "internal",
        "Hesap silinirken bir hata oluştu. Lütfen tekrar deneyin."
      );
    }
  });