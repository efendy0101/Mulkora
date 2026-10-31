import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

/**
 * Bildirim belgesi oluştuğunda FCM push notification gönderir.
 * docs/19-notification-system.md
 *
 * Firestore: users/{uid}/notifications/{notifId} oluştuğunda tetiklenir.
 */
export const onNotificationCreated = functions
  .region("europe-west1")
  .firestore.document("users/{uid}/notifications/{notifId}")
  .onCreate(async (snapshot, context) => {
    const notif = snapshot.data();
    const { uid } = context.params;

    if (!notif) {
      functions.logger.warn(`Bildirim verisi boş: ${context.params.notifId}`);
      return;
    }

    // Kullanıcının FCM tokenlarını al
    const userSnap = await admin.firestore().collection("users").doc(uid).get();

    if (!userSnap.exists) {
      functions.logger.warn(`Bildirim gönderilecek kullanıcı bulunamadı: ${uid}`);
      return;
    }

    const userData = userSnap.data()!;
    const fcmTokens: string[] = userData.fcmTokens ?? [];

    if (fcmTokens.length === 0) {
      functions.logger.info(`FCM token yok, bildirim gönderilmedi: ${uid}`);
      return;
    }

    // FCM mesajını hazırla
    const message: admin.messaging.MulticastMessage = {
      tokens: fcmTokens,
      notification: {
        title: notif.title ?? "Mülkora",
        body: notif.body ?? "",
      },
      data: {
        notifId: snapshot.id,
        type: notif.type ?? "general",
        deepLink: notif.deepLink ?? "",
        referenceId: notif.referenceId ?? "",
        fromUid: notif.fromUid ?? "",
      },
      android: {
        priority: "high",
        notification: {
          channelId: getChannelId(notif.type),
          clickAction: "FLUTTER_NOTIFICATION_CLICK",
        },
      },
    };

    try {
      const response = await admin.messaging().sendEachForMulticast(message);
      functions.logger.info(
        `Bildirim gönderildi: ${uid}, başarılı: ${response.successCount}, başarısız: ${response.failureCount}`
      );

      // Başarısız tokenları temizle
      const failedTokens: string[] = [];
      response.responses.forEach((resp, idx) => {
        if (!resp.success) {
          const errorCode = resp.error?.code;
          if (
            errorCode === "messaging/invalid-registration-token" ||
            errorCode === "messaging/registration-token-not-registered"
          ) {
            failedTokens.push(fcmTokens[idx]);
          }
        }
      });

      if (failedTokens.length > 0) {
        const validTokens = fcmTokens.filter((t) => !failedTokens.includes(t));
        await admin.firestore().collection("users").doc(uid).update({
          fcmTokens: validTokens,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        functions.logger.info(
          `Geçersiz ${failedTokens.length} token temizlendi: ${uid}`
        );
      }
    } catch (error) {
      functions.logger.error(`FCM gönderim hatası: ${uid}`, error);
    }
  });

/**
 * Bildirim türüne göre Android notification channel ID döndürür.
 *
 * Channel ID'leri MulkoraFirebaseMessagingService.kt ile birebir eşleşmeli:
 *   channel_reference  → CHANNEL_ID_REFERENCE
 *   channel_moderation → CHANNEL_ID_MODERATION
 *   channel_comments   → CHANNEL_ID_COMMENTS
 *   channel_contracts  → CHANNEL_ID_CONTRACTS
 *   channel_general    → CHANNEL_ID_GENERAL
 */
function getChannelId(type: string): string {
  switch (type) {
    case "reference_request":
    case "reference_accepted":
    case "reference_rejected":
    case "reference_response":
    case "reference_appeal":
      return "channel_reference";
    case "post_approved":
    case "post_rejected":
    case "post_edit_requested":
      return "channel_moderation";
    case "new_comment":
    case "new_reply":
      return "channel_comments";
    case "contract_invited":
    case "contract_updated":
    case "contract_approved":
    case "contract_pdf_ready":
      return "channel_contracts";
    default:
      return "channel_general";
  }
}