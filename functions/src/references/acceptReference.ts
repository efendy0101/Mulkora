import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * Referans davetini kabul eder (tanıma onayı).
 * docs/16-reference-system-state-machine.md
 * Durum geçişi: awaiting_confirmation → preparing  (CF-REF-002)
 *
 * Güvenlik kontrolleri:
 * - Yalnızca referansın alıcısı (toUid) kabul edebilir
 * - Referans "awaiting_confirmation" durumunda olmalıdır
 */
export const acceptReference = functions
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

    // Sadece alıcı kabul edebilir
    if (ref.toUid !== uid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Bu referansı kabul etme yetkiniz yoktur."
      );
    }

    // Yalnızca awaiting_confirmation durumunda kabul edilebilir
    if (ref.status !== "awaiting_confirmation") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Referans "awaiting_confirmation" durumunda değil: ${ref.status}`
      );
    }

    const now = admin.firestore.FieldValue.serverTimestamp();
    const batch = db.batch();

    // awaiting_confirmation → preparing (hedef "Evet" dedi)
    batch.update(refDoc, {
      status: "preparing",
      recognizedAt: now,
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
      type: "reference_recognized",
      title: "Referans kabul edildi",
      body: `${ref.toDisplayName} sizi tanıdığını onayladı. Şimdi referansınızı yazabilirsiniz.`,
      isRead: false,
      referenceId,
      deepLink: `/references/${referenceId}`,
      fromUid: uid,
      fromDisplayName: ref.toDisplayName,
      fromPhotoUrl: ref.toPhotoUrl ?? null,
      createdAt: now,
    });

    await batch.commit();

    functions.logger.info(
      `Referans tanındı (preparing): ${referenceId} (uid: ${uid})`
    );

    return { success: true, referenceId };
  });