import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * edit_requested durumundaki gönderiyi düzenleme için getirir.
 *
 * docs/17-post-moderation-state-machine.md:
 *   edit_requested → pending_approval geçişinde kullanılır.
 * CLAUDE.md §6: Gönderi yalnızca kendi sahibi tarafından,
 *   yalnızca edit_requested durumundayken düzenlenebilir.
 *
 * Dönüş değeri:
 * {
 *   postId: string,
 *   title: string,
 *   content: string,
 *   moderatorNote: string
 * }
 */
export const getPostForEdit = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
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

    // Yalnızca gönderi sahibi erişebilir
    if (post.authorId !== uid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Bu gönderiyi düzenleme yetkiniz yok."
      );
    }

    // Yalnızca edit_requested durumundaki gönderi düzenlenebilir
    if (post.status !== "edit_requested") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Gönderi düzenlenebilir durumda değil. Mevcut durum: ${post.status}`
      );
    }

    return {
      postId: postSnap.id,
      title: post.title ?? "",
      content: post.content ?? "",
      moderatorNote: post.moderatorNote ?? "",
    };
  });