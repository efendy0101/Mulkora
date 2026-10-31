import * as functions from "firebase-functions/v1";
import * as admin from "../firebaseAdmin";

/**
 * Şikayet oluşturma Cloud Function.
 * CLAUDE.md Bölüm 11: Şikâyet Sistemi.
 *
 * Kurallar:
 * - Kimlik doğrulanmış kullanıcılar şikayet edebilir.
 * - Aynı kullanıcı aynı içeriği yalnızca bir kez şikayet edebilir.
 * - Şikayet moderasyon kuyruğuna (reports koleksiyonu) eklenir.
 * - İçerik türü: post, comment, user, reference, reference_response.
 */

const VALID_CONTENT_TYPES = [
  "post",
  "comment",
  "user",
  "reference",
  "reference_response",
];

const VALID_REASONS = [
  "INSULT_OR_PROFANITY",
  "HARASSMENT",
  "THREAT",
  "HATE_SPEECH",
  "SPAM",
  "MISINFORMATION",
  "PERSONAL_INFO_SHARING",
  "DEFAMATION",
  "INAPPROPRIATE_IMAGE",
  "ADVERTISEMENT",
  "OFF_TOPIC",
  "FAKE_REFERENCE",
  "OTHER",
];

export const createReport = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Şikayet oluşturmak için giriş yapmanız gerekiyor."
      );
    }

    const uid = context.auth.uid;
    const { contentType, contentId, reason, additionalNote } = data as {
      contentType?: string;
      contentId?: string;
      reason?: string;
      additionalNote?: string;
    };

    // Doğrulama
    if (!contentType || !VALID_CONTENT_TYPES.includes(contentType)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçersiz içerik türü."
      );
    }
    if (!contentId || typeof contentId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "contentId gerekli."
      );
    }
    if (!reason || !VALID_REASONS.includes(reason)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçersiz şikayet nedeni."
      );
    }
    if (additionalNote && additionalNote.length > 1000) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Ek açıklama en fazla 1000 karakter olabilir."
      );
    }

    const db = admin.firestore();

    // Mükerrer şikayet kontrolü — aynı kullanıcı + aynı içerik
    const existingQuery = await db
      .collection("reports")
      .where("reporterUid", "==", uid)
      .where("contentId", "==", contentId)
      .where("contentType", "==", contentType)
      .limit(1)
      .get();

    if (!existingQuery.empty) {
      throw new functions.https.HttpsError(
        "already-exists",
        "Bu içeriği daha önce şikayet ettiniz."
      );
    }

    // Kullanıcı bilgisi
    const userDoc = await db.collection("users").doc(uid).get();
    const userData = userDoc.data();

    const reportRef = db.collection("reports").doc();
    await reportRef.set({
      reporterUid: uid,
      reporterUsername: userData?.username ?? "",
      contentType,
      contentId,
      reason,
      additionalNote: additionalNote?.trim() ?? "",
      status: "NEW",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // İçerik belgesine reportCount artır (isteğe bağlı - izleme için)
    try {
      let contentRef: admin.firestore.DocumentReference | null = null;
      if (contentType === "post") {
        contentRef = db.collection("posts").doc(contentId);
      } else if (contentType === "user") {
        contentRef = db.collection("users").doc(contentId);
      }
      if (contentRef) {
        await contentRef.update({
          reportCount: admin.firestore.FieldValue.increment(1),
        });
      }
    } catch {
      // Sayaç güncelleme hatası kritik değil, görmezden gel
    }

    functions.logger.info(
      `Yeni şikayet: ${reportRef.id} — ${contentType}/${contentId} — ${reason}`
    );

    return { success: true, reportId: reportRef.id };
  });