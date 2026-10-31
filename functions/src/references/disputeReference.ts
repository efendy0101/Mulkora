import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

const db = admin.firestore();

/**
 * Onaylanmış referansa itiraz eder.
 * docs/16-reference-system-state-machine.md §5 — İtiraz Durum Makinesi
 * Durum geçişi: approved → appealed  (CF-REF-006)
 *
 * Güvenlik kontrolleri:
 * - Yalnızca referansın alıcısı (toUid) itiraz edebilir
 * - Referans "approved" durumunda olmalıdır
 * - Daha önce itiraz edilmemiş olmalıdır
 *
 * docs/16 §7:
 * İtiraz sürecinde referans yayımda kalmaya devam eder
 * (itiraz kabul edilene kadar görünür).
 */
export const disputeReference = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const { referenceId, reason } = data;

    if (!referenceId || typeof referenceId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir referenceId gereklidir."
      );
    }

    if (!reason || typeof reason !== "string" || reason.trim().length < 10) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "İtiraz gerekçesi en az 10 karakter olmalıdır."
      );
    }

    if (reason.trim().length > 2000) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "İtiraz gerekçesi en fazla 2000 karakter olabilir."
      );
    }

    const refDoc = db.collection("references").doc(referenceId);
    const refSnap = await refDoc.get();

    if (!refSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Referans bulunamadı.");
    }

    const ref = refSnap.data()!;

    // Yalnızca referansın alıcısı (toUid) itiraz edebilir
    if (ref.toUid !== uid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Yalnızca referansı alan kişi itiraz edebilir."
      );
    }

    // Yalnızca "approved" durumundaki referanslara itiraz edilebilir
    if (ref.status !== "approved") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Referans "approved" durumunda değil: ${ref.status}`
      );
    }

    // Daha önce itiraz edilmiş mi?
    if (ref.disputeReason) {
      throw new functions.https.HttpsError(
        "already-exists",
        "Bu referansa zaten itiraz edilmiş."
      );
    }

    const now = admin.firestore.FieldValue.serverTimestamp();
    const batch = db.batch();

    // approved → appealed
    batch.update(refDoc, {
      status: "appealed",
      disputeReason: reason.trim(),
      appealedAt: now,
      updatedAt: now,
    });

    // Yöneticilere bildirim (toUid ve fromUid bilgisiyle)
    // Moderatör/admin panelinden incelenmek üzere referanslar koleksiyonuna itiraz kaydı
    const appealRef = db.collection("referenceAppeals").doc();
    batch.set(appealRef, {
      id: appealRef.id,
      referenceId,
      appellantUid: uid,
      fromUid: ref.fromUid,
      toUid: ref.toUid,
      fromDisplayName: ref.fromDisplayName,
      toDisplayName: ref.toDisplayName,
      reason: reason.trim(),
      status: "new",  // İtiraz Durum Makinesi: new → reviewing → upheld/rejected → closed
      createdAt: now,
      updatedAt: now,
    });

    await batch.commit();

    functions.logger.info(
      `Referans itirazı oluşturuldu: ${referenceId} (uid: ${uid})`
    );

    return { success: true, referenceId, appealId: appealRef.id };
  });