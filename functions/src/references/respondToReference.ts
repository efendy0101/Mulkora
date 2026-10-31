import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

const db = admin.firestore();

/**
 * Onaylı bir referansa yanıt yazar.
 * docs/16-reference-system-state-machine.md §4 — Referans Cevabı Durum Makinesi
 * Yalnızca "approved" durumundaki referanslara yanıt verilebilir.
 * Yanıt, referansın response alanına eklenir ve moderatör onayı bekler.
 */
export const respondToReference = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const { referenceId, responseContent } = data;

    if (!referenceId || typeof referenceId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir referenceId gereklidir."
      );
    }

    if (
      !responseContent ||
      typeof responseContent !== "string" ||
      responseContent.trim().length < 10
    ) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Yanıt en az 10 karakter olmalıdır."
      );
    }

    if (responseContent.trim().length > 1000) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Yanıt en fazla 1000 karakter olabilir."
      );
    }

    const refDoc = db.collection("references").doc(referenceId);
    const refSnap = await refDoc.get();

    if (!refSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Referans bulunamadı.");
    }

    const ref = refSnap.data()!;

    // Yalnızca referansı alan kişi yanıt verebilir
    if (ref.toUid !== uid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Yalnızca referansı alan kişi yanıt verebilir."
      );
    }

    // Yalnızca "approved" durumundaki referanslara yanıt verilebilir (§4)
    if (ref.status !== "approved") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Referans yanıt verilebilir durumda değil: ${ref.status}`
      );
    }

    // Daha önce yanıt verilmiş mi?
    if (ref.response !== null && ref.response !== undefined) {
      throw new functions.https.HttpsError(
        "already-exists",
        "Bu referansa zaten yanıt verilmiş."
      );
    }

    const now = admin.firestore.FieldValue.serverTimestamp();
    const responseId = db.collection("references").doc().id;

    const batch = db.batch();

    // Yanıt, moderasyon onayını bekler (docs/16 §4)
    batch.update(refDoc, {
      response: {
        id: responseId,
        respondentUid: uid,
        content: responseContent.trim(),
        status: "response_pending_review",
        createdAt: now,
      },
      updatedAt: now,
    });

    // Referansı verene bildirim: cevap moderasyon bekliyor
    const notifRef = db
      .collection("users")
      .doc(ref.fromUid)
      .collection("notifications")
      .doc();

    batch.set(notifRef, {
      id: notifRef.id,
      userId: ref.fromUid,
      type: "reference_response",
      title: "Referansınıza yanıt verildi",
      body: `${ref.toDisplayName} referansınıza yanıt yazdı. Moderatör onayından sonra görünür olacak.`,
      isRead: false,
      referenceId,
      deepLink: `/references/${referenceId}`,
      fromUid: uid,
      fromDisplayName: ref.toDisplayName,
      fromPhotoUrl: ref.toPhotoUrl ?? null,
      createdAt: now,
    });

    await batch.commit();

    functions.logger.info(`Referans yanıtı eklendi: ${referenceId} (${uid})`);

    return { success: true, referenceId };
  });