import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

const db = admin.firestore();

/**
 * Moderatör referans cevabını onaylar veya reddeder.
 * docs/16-reference-system-state-machine.md §4 — Referans Cevabı
 *
 * Durum geçişleri (response.status alanı):
 *   response_pending_review → visible   (action: "approve")
 *   response_pending_review → rejected  (action: "reject")
 *
 * Güvenlik kontrolleri:
 * - Yalnızca moderatör veya yönetici rolü
 * - Referans "approved" durumunda ve response.status = "response_pending_review" olmalı
 * - action: "approve" | "reject" zorunlu
 * - reject için moderatorNote zorunlu
 *
 * Yan etkiler:
 * - approve → referans sahibine bildirim
 * - reject → cevap sahibine bildirim
 * - Denetim kaydı oluşturulur
 */
export const moderateReferenceResponse = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const moderatorUid = context.auth.uid;
    const { referenceId, action, moderatorNote } = data;

    // --- Alan doğrulamaları ---
    if (!referenceId || typeof referenceId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir referenceId gereklidir."
      );
    }

    if (!action || !["approve", "reject"].includes(action)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        'action "approve" veya "reject" olmalıdır.'
      );
    }

    if (action === "reject" && (!moderatorNote || moderatorNote.trim().length < 5)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Reddetme işlemi için gerekçe (moderatorNote) girilmelidir."
      );
    }

    // --- Rol kontrolü ---
    const role = context.auth.token.role as string | undefined;
    if (!["moderator", "admin"].includes(role ?? "")) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Bu işlem için moderatör veya yönetici yetkisi gereklidir."
      );
    }

    // --- Referans belgesi ---
    const refDoc = db.collection("references").doc(referenceId);
    const refSnap = await refDoc.get();

    if (!refSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Referans bulunamadı.");
    }

    const ref = refSnap.data()!;

    if (ref.status !== "approved") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Referans "approved" durumunda değil: ${ref.status}`
      );
    }

    if (!ref.response || ref.response.status !== "response_pending_review") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "İncelenecek bir referans cevabı bulunamadı."
      );
    }

    const now = admin.firestore.FieldValue.serverTimestamp();
    const newResponseStatus = action === "approve" ? "visible" : "rejected";
    const batch = db.batch();

    // --- Cevap durumunu güncelle ---
    batch.update(refDoc, {
      "response.status": newResponseStatus,
      "response.moderatedAt": now,
      "response.moderatedBy": moderatorUid,
      "response.moderatorNote": moderatorNote?.trim() ?? null,
      updatedAt: now,
    });

    // --- Bildirimler ---
    if (action === "approve") {
      // Referans sahibine bildirim: cevap yayımlandı
      const notifRef = db
        .collection("users")
        .doc(ref.fromUid)
        .collection("notifications")
        .doc();

      batch.set(notifRef, {
        id: notifRef.id,
        userId: ref.fromUid,
        type: "reference_response_approved",
        title: "Referansınıza verilen cevap yayımlandı",
        body: `${ref.toDisplayName} tarafından referansınıza verilen cevap onaylandı.`,
        isRead: false,
        referenceId,
        deepLink: `/references/${referenceId}`,
        createdAt: now,
      });
    } else {
      // Cevap yazan kullanıcıya bildirim: cevap reddedildi
      const notifRef = db
        .collection("users")
        .doc(ref.toUid)
        .collection("notifications")
        .doc();

      batch.set(notifRef, {
        id: notifRef.id,
        userId: ref.toUid,
        type: "reference_response_rejected",
        title: "Referans cevabınız reddedildi",
        body: `Referansa verdiğiniz cevap moderatör tarafından reddedildi.`,
        isRead: false,
        referenceId,
        deepLink: `/references/${referenceId}`,
        createdAt: now,
      });
    }

    // --- Denetim kaydı ---
    const auditRef = db.collection("auditLogs").doc();
    batch.set(auditRef, {
      id: auditRef.id,
      adminUid: moderatorUid,
      action:
        action === "approve"
          ? "reference_response_approved"
          : "reference_response_rejected",
      targetId: referenceId,
      targetType: "referenceResponse",
      previousStatus: "response_pending_review",
      newStatus: newResponseStatus,
      note: moderatorNote?.trim() ?? null,
      timestamp: now,
    });

    await batch.commit();

    functions.logger.info(
      `Referans cevabı ${newResponseStatus}: ${referenceId} (moderatör: ${moderatorUid})`
    );

    return { success: true, referenceId, newResponseStatus };
  });