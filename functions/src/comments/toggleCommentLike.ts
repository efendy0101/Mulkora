import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

/**
 * Yorum beğeni toggle Cloud Function.
 * CLAUDE.md §10: Sayaçlar doğrudan değiştirilemez, CF kullanılır.
 *
 * Toggle mantığı:
 * - Kullanıcı daha önce beğenmemiş → likedBy dizisine ekle, likeCount+1
 * - Kullanıcı daha önce beğenmiş → likedBy dizisinden çıkar, likeCount-1
 *
 * Kendi yorumunu beğenme yasak.
 */
export const toggleCommentLike = functions
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

    // Kendi yorumunu beğenme
    if (commentData.authorUid === uid) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Kendi yorumunuzu beğenemezsiniz."
      );
    }

    const likedBy: string[] = Array.isArray(commentData.likedBy)
      ? commentData.likedBy
      : [];
    const alreadyLiked = likedBy.includes(uid);

    await commentRef.update({
      likedBy: alreadyLiked
        ? admin.firestore.FieldValue.arrayRemove(uid)
        : admin.firestore.FieldValue.arrayUnion(uid),
      likeCount: admin.firestore.FieldValue.increment(alreadyLiked ? -1 : 1),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return { success: true, liked: !alreadyLiked };
  });