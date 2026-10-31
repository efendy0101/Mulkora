import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

const db = admin.firestore();

/**
 * Moderatör gönderiyi onaylar.
 * docs/17-post-moderation-state-machine.md: pending → approved
 *
 * Güvenlik kontrolleri:
 * - Yalnızca moderatör veya admin rolü onaylayabilir
 * - Gönderi "pending" durumunda olmalıdır
 */
export const approvePost = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const moderatorUid = context.auth.uid;
    const { postId, note } = data;

    if (!postId || typeof postId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir postId gereklidir."
      );
    }

    // Rol kontrolü: moderatör veya admin
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

    // Batch: gönderi güncelle + yazar bildirim oluştur
    const batch = db.batch();

    // Gönderiyi onayla
    batch.update(postRef, {
      status: "approved",
      moderatedBy: moderatorUid,
      moderationNote: note ?? null,
      approvedAt: now,
      updatedAt: now,
    });

    // Yazar bildirimini oluştur
    const notifRef = db
      .collection("users")
      .doc(post.authorUid)
      .collection("notifications")
      .doc();

    batch.set(notifRef, {
      id: notifRef.id,
      userId: post.authorUid,
      type: "post_approved",
      title: "Gönderiniz onaylandı",
      body: `"${post.title}" başlıklı gönderiniz yayında!`,
      isRead: false,
      referenceId: postId,
      deepLink: `/posts/${postId}`,
      fromUid: moderatorUid,
      createdAt: now,
    });

    await batch.commit();

    functions.logger.info(
      `Gönderi onaylandı: ${postId} (moderatör: ${moderatorUid})`
    );

    return { success: true, postId };
  });