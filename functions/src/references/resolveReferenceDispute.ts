import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

const db = admin.firestore();

/**
 * Yönetici bir referans itirazını çözümler.
 *
 * CLAUDE.md §13: Referans sistemi — itiraz yönetimi.
 * docs/16-reference-system-state-machine.md — disputed → approved | rejected
 *
 * Güvenlik:
 * - Yalnızca admin veya moderatör rolü çağırabilir.
 * - Referans "disputed" durumunda olmalıdır.
 *
 * Parametre:
 *   - referenceId: string (zorunlu)
 *   - action: "uphold" | "overturn" (zorunlu)
 *     uphold   → itiraz reddedildi, referans approved kalır
 *     overturn → itiraz kabul edildi, referans rejected olur
 *   - adminNote: string (zorunlu, karar gerekçesi)
 *
 * Yan etkiler:
 *   - Her iki tarafa bildirim gönderilir.
 *   - Durum güncellemesi referans sahibinin profile yansıtılır.
 *   - auditLogs koleksiyonuna kayıt düşülür.
 */
export const resolveReferenceDispute = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Kimlik doğrulama gereklidir.");
    }

    const { referenceId, action, adminNote } = data as {
      referenceId: string;
      action: "uphold" | "overturn";
      adminNote: string;
    };

    if (!referenceId || !action || !adminNote?.trim()) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "referenceId, action ve adminNote zorunludur."
      );
    }

    if (!["uphold", "overturn"].includes(action)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "action 'uphold' veya 'overturn' olmalıdır."
      );
    }

    // Moderatör / admin kontrolü
    const adminUid = context.auth.uid;
    const adminRole = context.auth.token.role as string | undefined;
    if (!["moderator", "admin"].includes(adminRole ?? "")) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Yalnızca moderatör veya yönetici bu işlemi yapabilir."
      );
    }

    const referenceRef = db.collection("references").doc(referenceId);
    const referenceDoc = await referenceRef.get();

    if (!referenceDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Referans bulunamadı.");
    }

    const referenceData = referenceDoc.data()!;

    if (referenceData.status !== "disputed") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Referans itiraz durumunda değil. Mevcut durum: ${referenceData.status}`
      );
    }

    const newStatus = action === "uphold" ? "approved" : "rejected";
    const now = admin.firestore.FieldValue.serverTimestamp();

    await db.runTransaction(async (tx) => {
      // Referans durumunu güncelle
      tx.update(referenceRef, {
        status: newStatus,
        disputeResolution: action,
        disputeResolvedBy: adminUid,
        disputeResolvedAt: now,
        disputeAdminNote: adminNote.trim(),
        updatedAt: now,
      });

      // Karar gerekçesi: referansın verildiği kişi (toUid) bildirim alır
      const toNotifRef = db.collection("notifications").doc();
      tx.set(toNotifRef, {
        recipientUid: referenceData.toUid,
        type:
          action === "uphold"
            ? "reference_dispute_rejected"
            : "reference_dispute_accepted",
        referenceId,
        fromUid: adminUid,
        isRead: false,
        createdAt: now,
      });

      // Referansı yazan kişi (fromUid) bildirim alır
      const fromNotifRef = db.collection("notifications").doc();
      tx.set(fromNotifRef, {
        recipientUid: referenceData.fromUid,
        type:
          action === "uphold"
            ? "reference_dispute_rejected_author"
            : "reference_dispute_accepted_author",
        referenceId,
        fromUid: adminUid,
        isRead: false,
        createdAt: now,
      });

      // Audit log
      const auditRef = db.collection("auditLogs").doc();
      tx.set(auditRef, {
        action: "resolve_reference_dispute",
        performedBy: adminUid,
        targetCollection: "references",
        targetId: referenceId,
        details: {
          disputeAction: action,
          newStatus,
          adminNote: adminNote.trim(),
        },
        createdAt: now,
      });

      // Profil skoru güncellenmesi: overturn ise referansı kaldır
      if (action === "overturn") {
        const toUserRef = db.collection("users").doc(referenceData.toUid);
        tx.update(toUserRef, {
          referenceCount: admin.firestore.FieldValue.increment(-1),
          updatedAt: now,
        });
      }
    });

    return {
      success: true,
      newStatus,
      message:
        action === "uphold"
          ? "İtiraz reddedildi, referans onaylı kalmaya devam ediyor."
          : "İtiraz kabul edildi, referans kaldırıldı.",
    };
  });