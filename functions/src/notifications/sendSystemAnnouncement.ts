import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

/**
 * sendSystemAnnouncement — Yönetici duyurusu push bildirimi gönderir.
 *
 * CLAUDE.md §16: Yalnızca admin rolü çağırabilir.
 * docs/19-notification-system.md §4: Topik bazlı sistem bildirimleri
 *
 * Alıcı segmentleri FCM topic'e eşlenir:
 *   "all"                        → /topics/all_users
 *   "tenants"                    → /topics/role_tenant
 *   "landlords"                  → /topics/role_landlord
 *   "all_tenants_and_landlords"  → /topics/role_tenant ve /topics/role_landlord
 */

type RecipientType = "all" | "tenants" | "landlords" | "all_tenants_and_landlords";

interface SendAnnouncementData {
  title: string;
  body: string;
  recipient: RecipientType;
  deepLink?: string | null;
}

const TOPIC_MAP: Record<Exclude<RecipientType, "all_tenants_and_landlords">, string> = {
  all: "all_users",
  tenants: "role_tenant",
  landlords: "role_landlord",
};

export const sendSystemAnnouncement = functions
  .region("europe-west1")
  .https.onCall(async (data: SendAnnouncementData, context) => {
    // Yalnızca admin
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Kimlik doğrulaması gerekli.");
    }
    const callerRole = context.auth.token?.role as string | undefined;
    if (callerRole !== "admin") {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Bu işlem yalnızca yöneticiler tarafından gerçekleştirilebilir."
      );
    }

    const { title, body, recipient, deepLink } = data;

    if (!title?.trim() || !body?.trim()) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Başlık ve mesaj alanları zorunludur."
      );
    }
    if (body.length > 200) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Mesaj en fazla 200 karakter olabilir."
      );
    }

    const validRecipients: RecipientType[] = [
      "all",
      "tenants",
      "landlords",
      "all_tenants_and_landlords",
    ];
    if (!validRecipients.includes(recipient)) {
      throw new functions.https.HttpsError("invalid-argument", "Geçersiz alıcı tipi.");
    }

    const notification: admin.messaging.Notification = {
      title: title.trim(),
      body: body.trim(),
    };

    const apns: admin.messaging.ApnsConfig = {
      payload: {
        aps: {
          sound: "default",
          badge: 1,
        },
      },
    };

    const android: admin.messaging.AndroidConfig = {
      priority: "normal",
      notification: {
        sound: "default",
        channelId: "system_announcements",
      },
    };

    const dataPayload: Record<string, string> = {
      type: "system_announcement",
      ...(deepLink ? { deepLink } : {}),
    };

    let sentCount = 0;

    if (recipient === "all_tenants_and_landlords") {
      // İki topik için ayrı mesaj gönder
      const [tenantResult, landlordResult] = await Promise.all([
        admin.messaging().send({
          topic: TOPIC_MAP.tenants,
          notification,
          apns,
          android,
          data: dataPayload,
        }),
        admin.messaging().send({
          topic: TOPIC_MAP.landlords,
          notification,
          apns,
          android,
          data: dataPayload,
        }),
      ]);
      sentCount = 2; // Topik başarılı gönderim sayısı (gerçek cihaz sayısı FCM tarafında bilinir)
      functions.logger.info("System announcement sent to tenant+landlord topics", {
        tenantResult,
        landlordResult,
        callerUid: context.auth.uid,
      });
    } else {
      const topic = TOPIC_MAP[recipient];
      const result = await admin.messaging().send({
        topic,
        notification,
        apns,
        android,
        data: dataPayload,
      });
      sentCount = 1;
      functions.logger.info("System announcement sent", {
        topic,
        result,
        callerUid: context.auth.uid,
      });
    }

    // Denetim kaydı
    await admin.firestore().collection("auditLogs").add({
      action: "send_system_announcement",
      performedBy: context.auth.uid,
      details: {
        title: title.trim(),
        body: body.trim(),
        recipient,
        deepLink: deepLink ?? null,
      },
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return { success: true, sentCount };
  });