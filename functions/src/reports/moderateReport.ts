import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * Moderatör bir şikayeti çözüme kavuşturur.
 *
 * CLAUDE.md §11: Şikayet sistemi — 5 içerik türü, 13 neden.
 * docs/11-firebase-collections.md — reports koleksiyonu.
 *
 * Güvenlik:
 * - Yalnızca admin veya moderatör rolü çağırabilir.
 * - reports belgesi "new" veya "reviewed" durumunda olmalıdır.
 *
 * Parametreler:
 *   - reportId: string (zorunlu)
 *   - resolution: string (zorunlu, karar açıklaması)
 *   - action: "warn" | "remove_content" | "ban_temporary" | "ban_permanent" | "no_action"
 *
 * Yan etkiler:
 *   - reports belgesi "resolved" olarak güncellenir.
 *   - action === "remove_content" ise hedef içerik silinir/hidden yapılır.
 *   - action === "ban_temporary" | "ban_permanent" ise kullanıcı yasaklanır.
 *   - Şikayet edene ve edilen kullanıcıya bildirim gönderilir.
 *   - auditLogs koleksiyonuna kayıt düşülür.
 */
export const resolveReport = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Kimlik doğrulama gereklidir.");
    }

    const { reportId, resolution, action } = data as {
      reportId: string;
      resolution: string;
      action: string;
    };

    if (!reportId || !resolution?.trim()) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "reportId ve resolution zorunludur."
      );
    }

    const validActions = ["warn", "remove_content", "ban_temporary", "ban_permanent", "no_action"];
    if (action && !validActions.includes(action)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        `Geçersiz action: ${action}. Kabul edilen: ${validActions.join(", ")}`
      );
    }

    // Moderatör / admin kontrolü
    const moderatorUid = context.auth.uid;
    const moderatorRole = context.auth.token.role as string | undefined;
    if (!["moderator", "admin"].includes(moderatorRole ?? "")) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Yalnızca moderatör veya yönetici bu işlemi yapabilir."
      );
    }

    const reportRef = db.collection("reports").doc(reportId);
    const reportDoc = await reportRef.get();

    if (!reportDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Şikayet bulunamadı.");
    }

    const reportData = reportDoc.data()!;

    if (!["new", "reviewed"].includes(reportData.status)) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Şikayet zaten işlenmiş. Mevcut durum: ${reportData.status}`
      );
    }

    const now = admin.firestore.FieldValue.serverTimestamp();

    await db.runTransaction(async (tx) => {
      // Raporu "resolved" olarak güncelle
      tx.update(reportRef, {
        status: "resolved",
        resolution: resolution.trim(),
        resolvedAction: action ?? "no_action",
        resolvedBy: moderatorUid,
        resolvedAt: now,
        updatedAt: now,
      });

      // İçerik kaldırma işlemi
      if (action === "remove_content" && reportData.targetId) {
        const targetType = reportData.targetType as string;
        if (targetType === "post") {
          const postRef = db.collection("posts").doc(reportData.targetId);
          tx.update(postRef, { status: "removed", updatedAt: now });
        } else if (targetType === "comment") {
          const commentRef = db.collection("comments").doc(reportData.targetId);
          tx.update(commentRef, { isDeleted: true, deletedAt: now, deletedBy: moderatorUid });
        } else if (targetType === "reference" || targetType === "reference_response") {
          const refRef = db.collection("references").doc(reportData.targetId);
          tx.update(refRef, { status: "rejected", updatedAt: now });
        }
      }

      // Kullanıcı yasağı
      if (action === "ban_temporary" && reportData.targetOwnerUid) {
        const bannedUntil = new Date();
        bannedUntil.setDate(bannedUntil.getDate() + 7);
        const userRef = db.collection("users").doc(reportData.targetOwnerUid);
        tx.update(userRef, {
          isBanned: true,
          banReason: "Şikayet sonrası moderatör kararı",
          bannedUntil: admin.firestore.Timestamp.fromDate(bannedUntil),
          updatedAt: now,
        });
      } else if (action === "ban_permanent" && reportData.targetOwnerUid) {
        const userRef = db.collection("users").doc(reportData.targetOwnerUid);
        tx.update(userRef, {
          isBanned: true,
          banReason: "Kalıcı yasak — şikayet sonrası moderatör kararı",
          bannedUntil: null,
          updatedAt: now,
        });
      }

      // Şikayet edene bildirim
      const reporterNotifRef = db.collection("notifications").doc();
      tx.set(reporterNotifRef, {
        recipientUid: reportData.reporterUid,
        type: "report_resolved",
        reportId,
        isRead: false,
        createdAt: now,
      });

      // Audit log
      const auditRef = db.collection("auditLogs").doc();
      tx.set(auditRef, {
        action: "resolve_report",
        performedBy: moderatorUid,
        targetCollection: "reports",
        targetId: reportId,
        details: {
          resolution: resolution.trim(),
          resolvedAction: action ?? "no_action",
          targetType: reportData.targetType,
          targetId: reportData.targetId,
        },
        createdAt: now,
      });
    });

    return {
      success: true,
      message: "Şikayet başarıyla çözüldü.",
    };
  });

/**
 * Moderatör bir şikayeti reddeder (geçersiz/yanlış şikayet).
 *
 * Parametreler:
 *   - reportId: string (zorunlu)
 *   - reason: string (zorunlu, reddetme gerekçesi)
 */
export const dismissReport = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Kimlik doğrulama gereklidir.");
    }

    const { reportId, reason } = data as {
      reportId: string;
      reason: string;
    };

    if (!reportId || !reason?.trim()) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "reportId ve reason zorunludur."
      );
    }

    // Moderatör / admin kontrolü
    const moderatorUid = context.auth.uid;
    const moderatorRole = context.auth.token.role as string | undefined;
    if (!["moderator", "admin"].includes(moderatorRole ?? "")) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Yalnızca moderatör veya yönetici bu işlemi yapabilir."
      );
    }

    const reportRef = db.collection("reports").doc(reportId);
    const reportDoc = await reportRef.get();

    if (!reportDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Şikayet bulunamadı.");
    }

    const reportData = reportDoc.data()!;

    if (!["new", "reviewed"].includes(reportData.status)) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Şikayet zaten işlenmiş. Mevcut durum: ${reportData.status}`
      );
    }

    const now = admin.firestore.FieldValue.serverTimestamp();

    await db.runTransaction(async (tx) => {
      tx.update(reportRef, {
        status: "dismissed",
        dismissReason: reason.trim(),
        dismissedBy: moderatorUid,
        dismissedAt: now,
        updatedAt: now,
      });

      // Audit log
      const auditRef = db.collection("auditLogs").doc();
      tx.set(auditRef, {
        action: "dismiss_report",
        performedBy: moderatorUid,
        targetCollection: "reports",
        targetId: reportId,
        details: {
          dismissReason: reason.trim(),
          targetType: reportData.targetType,
          targetId: reportData.targetId,
        },
        createdAt: now,
      });
    });

    return {
      success: true,
      message: "Şikayet reddedildi.",
    };
  });