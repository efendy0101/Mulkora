import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * Referans davetini reddeder (tanımıyorum).
 * docs/16-reference-system-state-machine.md
 * Durum geçişi: awaiting_confirmation → not_recognized  (CF-REF-002)
 *
 * Güvenlik kontrolleri:
 * - Yalnızca referansın alıcısı (toUid) reddedebilir
 * - Referans "awaiting_confirmation" durumunda olmalıdır
 */
export const rejectReference = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const { referenceId } = data;

    if (!referenceId || typeof referenceId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir referenceId gereklidir."
      );
    }

    const refDoc = db.collection("references").doc(referenceId);
    const refSnap = await refDoc.get();

    if (!refSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Referans bulunamadı.");
    }

    const ref = refSnap.data()!;

    if (ref.toUid !== uid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Bu referansı reddetme yetkiniz yoktur."
      );
    }

    // Yalnızca awaiting_confirmation durumunda reddedilebilir
    if (ref.status !== "awaiting_confirmation") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Referans "awaiting_confirmation" durumunda değil: ${ref.status}`
      );
    }

    const now = admin.firestore.FieldValue.serverTimestamp();
    const batch = db.batch();

    // awaiting_confirmation → not_recognized (hedef "Hayır" dedi)
    batch.update(refDoc, {
      status: "not_recognized",
      notRecognizedAt: now,
      updatedAt: now,
    });

    // Referansı başlatan kişiye bildirim
    const notifRef = db
      .collection("users")
      .doc(ref.fromUid)
      .collection("notifications")
      .doc();

    batch.set(notifRef, {
      id: notifRef.id,
      userId: ref.fromUid,
      type: "reference_not_recognized",
      title: "Referans tanınmadı",
      body: `${ref.toDisplayName} sizi tanımadığını belirtti. Referans süreci sonlandı.`,
      isRead: false,
      referenceId,
      deepLink: `/references/${referenceId}`,
      fromUid: uid,
      fromDisplayName: ref.toDisplayName,
      fromPhotoUrl: ref.toPhotoUrl ?? null,
      createdAt: now,
    });

    await batch.commit();

    functions.logger.info(`Referans reddedildi: ${referenceId} (${uid})`);

    return { success: true, referenceId };
  });