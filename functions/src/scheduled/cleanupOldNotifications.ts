import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * 30 günden eski okunmuş bildirimleri temizler.
 * docs/19-notification-system.md: Bildirim saklama politikası
 *
 * Her gece 03:00'da çalışır (Europe/Istanbul = UTC+3 → 00:00 UTC).
 */
export const cleanupOldNotifications = functions
  .region("europe-west1")
  .pubsub.schedule("0 0 * * *")
  .timeZone("Europe/Istanbul")
  .onRun(async () => {
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

    const cutoffTimestamp = admin.firestore.Timestamp.fromDate(thirtyDaysAgo);
    let totalDeleted = 0;

    try {
      // Tüm kullanıcıların okunmuş eski bildirimlerini temizle
      // NOT: Firestore collectionGroup query ile tüm alt koleksiyonlarda arama
      const oldNotifSnap = await db
        .collectionGroup("notifications")
        .where("isRead", "==", true)
        .where("createdAt", "<", cutoffTimestamp)
        .limit(500)
        .get();

      if (oldNotifSnap.empty) {
        functions.logger.info("Temizlenecek eski bildirim yok.");
        return null;
      }

      // Batch silme (max 500 işlem / batch)
      const batch = db.batch();
      oldNotifSnap.docs.forEach((doc) => {
        batch.delete(doc.ref);
      });

      await batch.commit();
      totalDeleted = oldNotifSnap.size;

      functions.logger.info(
        `Eski bildirim temizleme tamamlandı: ${totalDeleted} bildirim silindi.`
      );
    } catch (error) {
      functions.logger.error("Bildirim temizleme hatası:", error);
    }

    return null;
  });