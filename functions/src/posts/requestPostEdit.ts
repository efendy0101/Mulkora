import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * Moderatör gönderi için düzenleme ister.
 * docs/17-post-moderation-state-machine.md: pending → edit_requested
 */
export const requestPostEdit = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const moderatorUid = context.auth.uid;
    const { postId, editNote } = data;

    if (!postId || typeof postId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir postId gereklidir."
      );
    }

    if (!editNote || typeof editNote !== "string" || editNote.trim().length < 10) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Düzenleme notu en az 10 karakter olmalıdır."
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

    batch.update(postRef, {
      status: "edit_requested",
      moderatedBy: moderatorUid,
      editNote: editNote.trim(),
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
      type: "post_edit_requested",
      title: "Gönderiniz için düzenleme istendi",
      body: `"${post.title}" başlıklı gönderiniz için düzenleme isteniyor: ${editNote.trim()}`,
      isRead: false,
      referenceId: postId,
      deepLink: `/posts/${postId}/edit`,
      fromUid: moderatorUid,
      createdAt: now,
    });

    await batch.commit();

    functions.logger.info(
      `Gönderi düzenleme istendi: ${postId} (moderatör: ${moderatorUid})`
    );

    return { success: true, postId };
  });