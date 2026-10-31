import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

const db = admin.firestore();

/**
 * Moderatör referansı onaylar veya reddeder.
 * docs/16-reference-system-state-machine.md §3 — Geçiş Tablosu
 * Durum geçişleri:
 *   pending_approval → approved  (CF-POST-002 variant)
 *   pending_approval → rejected
 *
 * Güvenlik kontrolleri:
 * - Yalnızca moderatör veya yönetici rolüne sahip kullanıcılar çağırabilir
 * - Referans "pending_approval" durumunda olmalıdır
 * - action: "approve" | "reject" zorunludur
 * - reject için moderatorNote (red gerekçesi) zorunludur
 *
 * Yan etkiler:
 * - approved → her iki tarafa bildirim, referans profilde yayımlanır
 * - rejected → referans yazana bildirim
 * - Denetim kaydı (auditLogs) oluşturulur
 */
export const moderateReference = functions
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

    // --- Rol kontrolü: moderatör veya yönetici ---
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

    if (ref.status !== "pending_approval") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Referans "pending_approval" durumunda değil: ${ref.status}`
      );
    }

    const now = admin.firestore.FieldValue.serverTimestamp();
    const newStatus = action === "approve" ? "approved" : "rejected";
    const batch = db.batch();

    // --- Durum güncellemesi ---
    batch.update(refDoc, {
      status: newStatus,
      moderatedAt: now,
      moderatedBy: moderatorUid,
      moderatorNote: moderatorNote?.trim() ?? null,
      updatedAt: now,
    });

    // --- Bildirimler ---
    if (action === "approve") {
      // Her iki tarafa bildirim: referans yayımlandı
      const notifFromRef = db
        .collection("users")
        .doc(ref.fromUid)
        .collection("notifications")
        .doc();

      batch.set(notifFromRef, {
        id: notifFromRef.id,
        userId: ref.fromUid,
        type: "reference_approved",
        title: "Referansınız yayımlandı",
        body: `${ref.toDisplayName} adlı kullanıcı için verdiğiniz referans onaylandı ve profilde yayımlandı.`,
        isRead: false,
        referenceId,
        deepLink: `/references/${referenceId}`,
        createdAt: now,
      });

      const notifToRef = db
        .collection("users")
        .doc(ref.toUid)
        .collection("notifications")
        .doc();

      batch.set(notifToRef, {
        id: notifToRef.id,
        userId: ref.toUid,
        type: "reference_approved",
        title: "Hakkınızda bir referans yayımlandı",
        body: `${ref.fromDisplayName} tarafından hakkınızda yazılan referans onaylandı.`,
        isRead: false,
        referenceId,
        deepLink: `/references/${referenceId}`,
        createdAt: now,
      });
    } else {
      // Yalnızca referans yazana bildirim: referans reddedildi
      const notifRef = db
        .collection("users")
        .doc(ref.fromUid)
        .collection("notifications")
        .doc();

      batch.set(notifRef, {
        id: notifRef.id,
        userId: ref.fromUid,
        type: "reference_rejected",
        title: "Referansınız reddedildi",
        body: `${ref.toDisplayName} adlı kullanıcı için yazdığınız referans moderatör tarafından reddedildi.`,
        isRead: false,
        referenceId,
        deepLink: `/references/${referenceId}`,
        createdAt: now,
      });
    }

    // --- Denetim kaydı (auditLogs) ---
    const auditRef = db.collection("auditLogs").doc();
    batch.set(auditRef, {
      id: auditRef.id,
      adminUid: moderatorUid,
      action: action === "approve" ? "reference_approved" : "reference_rejected",
      targetId: referenceId,
      targetType: "reference",
      previousStatus: "pending_approval",
      newStatus,
      note: moderatorNote?.trim() ?? null,
      timestamp: now,
    });

    await batch.commit();

    functions.logger.info(
      `Referans ${newStatus}: ${referenceId} (moderatör: ${moderatorUid})`
    );

    return { success: true, referenceId, newStatus };
  });
