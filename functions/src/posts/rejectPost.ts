import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

const db = admin.firestore();

/**
 * Moderatör gönderiyi reddeder.
 * docs/17-post-moderation-state-machine.md: pending → rejected
 */
export const rejectPost = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const moderatorUid = context.auth.uid;
    const { postId, reason } = data;

    if (!postId || typeof postId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir postId gereklidir."
      );
    }

    if (!reason || typeof reason !== "string" || reason.trim().length < 10) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Red nedeni en az 10 karakter olmalıdır."
      );
    }

    // Rol kontrolü
    const role = context.auth.token.role as string | undefined;

    if (role !== "moderator" && role !== "admin") {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Bu işlem için moderatör veya admin rolü gereklidir."
      );
    }

    const postRef = db.collection("posts").doc(postId);
    const postSnap = await postRef.get();

    if (!postSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Gönderi bulunamadı.");
    }

    const post = postSnap.data()!;

    if (post.status !== "pending") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Gönderi "pending" durumunda değil: ${post.status}`
      );
    }

    const now = admin.firestore.FieldValue.serverTimestamp();
    const batch = db.batch();

    // Gönderiyi reddet
    batch.update(postRef, {
      status: "rejected",
      moderatedBy: moderatorUid,
      rejectionReason: reason.trim(),
      updatedAt: now,
    });

    // Yazar bildirimi
    const notifRef = db
      .collection("users")
      .doc(post.authorUid)
      .collection("notifications")
      .doc();

    batch.set(notifRef, {
      id: notifRef.id,
      userId: post.authorUid,
      type: "post_rejected",
      title: "Gönderiniz reddedildi",
      body: `"${post.title}" başlıklı gönderiniz reddedildi. Neden: ${reason.trim()}`,
      isRead: false,
      referenceId: postId,
      deepLink: `/posts/${postId}`,
      fromUid: moderatorUid,
      createdAt: now,
    });

    await batch.commit();

    functions.logger.info(
      `Gönderi reddedildi: ${postId} (moderatör: ${moderatorUid})`
    );

    return { success: true, postId };
  });