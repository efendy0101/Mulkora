import * as functions from "firebase-functions/v1";
import * as admin from "../firebaseAdmin";

/**
 * Yorum silme Cloud Function.
 * CLAUDE.md Bölüm 9: Yorum ve Cevap Sistemi.
 *
 * Yetkiler:
 * - Yorum sahibi kendi yorumunu silebilir.
 * - Moderatör/Admin herhangi bir yorumu silebilir.
 * Sayaçlar atomik olarak azaltılır.
 */
export const deleteComment = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Giriş yapmanız gerekiyor."
      );
    }

    const uid = context.auth.uid;
    const { postId, commentId } = data as {
      postId?: string;
      commentId?: string;
    };

    if (!postId || !commentId) {
      throw new functions.https.HttpsError("invalid-argument", "postId ve commentId gerekli.");
    }

    const db = admin.firestore();

    // Yorum belgesini getir
    const commentRef = db
      .collection("posts")
      .doc(postId)
      .collection("comments")
      .doc(commentId);
    const commentDoc = await commentRef.get();

    if (!commentDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Yorum bulunamadı.");
    }

    const commentData = commentDoc.data()!;

    // Yetki kontrolü: yazar mı yoksa moderatör/admin mı?
    const isAuthor = commentData.authorUid === uid;
    const role = context.auth.token.role as string | undefined;
    const isModerator = role === "moderator" || role === "admin";

    if (!isAuthor && !isModerator) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Bu yorumu silme yetkiniz yok."
      );
    }

    const batch = db.batch();

    // Yorumu sil
    batch.delete(commentRef);

    const parentId = commentData.parentId as string | null;

    if (!parentId) {
      // Üst düzey yorum: postun commentCount'unu azalt
      batch.update(db.collection("posts").doc(postId), {
        commentCount: admin.firestore.FieldValue.increment(-1),
      });
    } else {
      // Cevap: parent yorumun replyCount'unu azalt
      batch.update(
        db
          .collection("posts")
          .doc(postId)
          .collection("comments")
          .doc(parentId),
        { replyCount: admin.firestore.FieldValue.increment(-1) }
      );
    }

    // Moderatör siliyorsa audit log
    if (isModerator && !isAuthor) {
      const auditRef = db.collection("auditLogs").doc();
      batch.set(auditRef, {
        action: "DELETE_COMMENT",
        moderatorUid: uid,
        targetUid: commentData.authorUid,
        postId,
        commentId,
        reason: "Moderatör tarafından silindi",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    await batch.commit();

    return { success: true };
  });