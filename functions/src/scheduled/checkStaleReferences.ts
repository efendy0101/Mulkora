import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * Askıda kalan (stale) referansları kontrol eder.
 * docs/16-reference-system-state-machine.md §6 — Zaman Aşımı Kuralları
 *
 * Kurallar:
 * - 7 günden uzun "invite_sent" kalırsa → not_recognized (son durum)
 * - 30 günden uzun "awaiting_confirmation" kalırsa → not_recognized
 * - 14 günden uzun "preparing" kalırsa → deleted (moderatör kararı)
 *
 * Her gün 04:00'da çalışır (Türkiye saatiyle).
 */
export const checkStaleReferences = functions
  .region("europe-west1")
  .pubsub.schedule("0 4 * * *")
  .timeZone("Europe/Istanbul")
  .onRun(async () => {
    const now = admin.firestore.FieldValue.serverTimestamp();
    let processedCount = 0;

    try {
      // --- 1. invite_sent → 7 gün geçtiyse not_recognized ---
      const sevenDaysAgo = new Date();
      sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

      const staleInvitesSnap = await db
        .collection("references")
        .where("status", "==", "invite_sent")
        .where("createdAt", "<", admin.firestore.Timestamp.fromDate(sevenDaysAgo))
        .limit(200)
        .get();

      for (const refDoc of staleInvitesSnap.docs) {
        const ref = refDoc.data();
        const batch = db.batch();

        // invite_sent → not_recognized (zaman aşımı)
        batch.update(refDoc.ref, {
          status: "not_recognized",
          notRecognizedAt: now,
          notRecognizedReason: "timeout",
          updatedAt: now,
        });

        // Gönderene bildirim
        const notifRef = db
          .collection("users")
          .doc(ref.fromUid)
          .collection("notifications")
          .doc();

        batch.set(notifRef, {
          id: notifRef.id,
          userId: ref.fromUid,
          type: "reference_timeout",
          title: "Referans daveti yanıtsız kaldı",
          body: `${ref.toDisplayName} adlı kullanıcıya gönderdiğiniz referans daveti 7 gün içinde yanıtlanmadı.`,
          isRead: false,
          referenceId: refDoc.id,
          deepLink: `/references/${refDoc.id}`,
          fromUid: "system",
          createdAt: now,
        });

        await batch.commit();
        processedCount++;
      }

      // --- 2. awaiting_confirmation → 30 gün geçtiyse not_recognized ---
      const thirtyDaysAgo = new Date();
      thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

      const staleAwaitingSnap = await db
        .collection("references")
        .where("status", "==", "awaiting_confirmation")
        .where("createdAt", "<", admin.firestore.Timestamp.fromDate(thirtyDaysAgo))
        .limit(100)
        .get();

      for (const refDoc of staleAwaitingSnap.docs) {
        const ref = refDoc.data();
        const batch = db.batch();

        batch.update(refDoc.ref, {
          status: "not_recognized",
          notRecognizedAt: now,
          notRecognizedReason: "confirmation_timeout",
          updatedAt: now,
        });

        const notifRef = db
          .collection("users")
          .doc(ref.fromUid)
          .collection("notifications")
          .doc();

        batch.set(notifRef, {
          id: notifRef.id,
          userId: ref.fromUid,
          type: "reference_timeout",
          title: "Referans daveti sona erdi",
          body: `${ref.toDisplayName} adlı kullanıcıya gönderdiğiniz referans daveti onaylanmadı.`,
          isRead: false,
          referenceId: refDoc.id,
          deepLink: `/references/${refDoc.id}`,
          fromUid: "system",
          createdAt: now,
        });

        await batch.commit();
        processedCount++;
      }

      functions.logger.info(
        `Stale referans kontrolü tamamlandı: ${processedCount} referans güncellendi.`
      );
    } catch (error) {
      functions.logger.error("Stale referans kontrol hatası:", error);
    }

    return null;
  });