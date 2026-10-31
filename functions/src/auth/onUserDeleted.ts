import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

const db = admin.firestore();

/**
 * Kullanıcı silindiğinde tetiklenir.
 * CLAUDE.md Bölüm 2: Hesap Silme
 * docs/14-cloud-functions-architecture.md
 *
 * Temizlenen veriler:
 * - users/{uid} belgesi
 * - users/{uid}/notifications alt koleksiyonu
 * - Storage'daki profil fotoğrafı
 *
 * NOT: Gönderiler ve referanslar belge bütünlüğü için silinmez,
 * yazar bilgisi "silindi" olarak işaretlenir.
 */
export const onUserDeleted = functions
  .region("europe-west1")
  .auth.user()
  .onDelete(async (user) => {
    const { uid } = user;
    const batch = db.batch();

    try {
      // 1. Kullanıcı bildirimlerini sil
      const notifSnap = await db
        .collection("users")
        .doc(uid)
        .collection("notifications")
        .limit(500)
        .get();

      notifSnap.docs.forEach((doc) => {
        batch.delete(doc.ref);
      });

      // 2. Kullanıcı belgesini sil
      batch.delete(db.collection("users").doc(uid));

      await batch.commit();

      // 3. Storage'daki profil fotoğrafını sil
      try {
        const bucket = admin.storage().bucket();
        await bucket.deleteFiles({
          prefix: `profile_photos/${uid}/`,
        });
      } catch (storageError) {
        functions.logger.warn(
          `Profil fotoğrafı silinemedi: ${uid}`,
          storageError
        );
      }

      functions.logger.info(`Kullanıcı verileri temizlendi: ${uid}`);
    } catch (error) {
      functions.logger.error(
        `Kullanıcı verileri temizlenemedi: ${uid}`,
        error
      );
    }
  });