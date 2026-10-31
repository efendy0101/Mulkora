import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * Gönderi incelemeye gönderilir.
 * CLAUDE.md Bölüm 7: Gönderi Oluşturma
 * docs/17-post-moderation-state-machine.md: draft → pending
 *
 * Güvenlik kontrolleri:
 * - Yalnızca gönderi sahibi çağırabilir
 * - Gönderi "draft" durumunda olmalıdır
 * - E-posta doğrulanmış olmalıdır
 */
export const submitPost = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    // Kimlik doğrulama kontrolü
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const { postId } = data;

    if (!postId || typeof postId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir postId gereklidir."
      );
    }

    const postRef = db.collection("posts").doc(postId);
    const postSnap = await postRef.get();

    if (!postSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Gönderi bulunamadı.");
    }

    const post = postSnap.data()!;

    // Yetki kontrolü: Yalnızca sahibi gönderebilir
    if (post.authorUid !== uid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Bu gönderiyi gönderme yetkiniz yoktur."
      );
    }

    // Durum kontrolü: draft veya edit_requested olmalı
    if (post.status !== "draft" && post.status !== "edit_requested") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Gönderi "draft" veya "edit_requested" durumunda değil: ${post.status}`
      );
    }

    // E-posta doğrulama kontrolü
    const userRecord = await admin.auth().getUser(uid);
    if (!userRecord.emailVerified) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Gönderi gönderebilmek için e-postanızı doğrulamanız gereklidir."
      );
    }

    // Durumu "pending" olarak güncelle
    await postRef.update({
      status: "pending",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      submittedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`Gönderi incelemeye gönderildi: ${postId} (uid: ${uid})`);

    return { success: true, postId };
  });