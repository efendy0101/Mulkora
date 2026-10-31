import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

const db = admin.firestore();

/**
 * Gönderi içeriğini günceller (edit_requested durumunda).
 *
 * CF-POST-006: docs/14-cloud-functions-architecture.md
 * docs/17-post-moderation-state-machine.md:
 *   edit_requested → pending_approval (kullanıcı düzenleme gönderdiğinde)
 *
 * CLAUDE.md §6: Gönderi düzenleme yalnızca edit_requested durumundaki gönderilerde mümkündür.
 * Yazara ait olmayan bir gönderi düzenlenemez (Cloud Function + Security Rules çift güvence).
 */
export const updatePost = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const { postId, title, content } = data;

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

    // Yalnızca gönderinin sahibi düzenleyebilir
    if (post.authorId !== uid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Yalnızca gönderinin sahibi düzenleme yapabilir."
      );
    }

    // Yalnızca edit_requested durumundaki gönderiler düzenlenebilir
    if (post.status !== "edit_requested") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Gönderi yalnızca düzenleme talep edilmiş durumdayken güncellenebilir."
      );
    }

    // Validasyon
    if (title !== undefined) {
      if (typeof title !== "string" || title.trim().length < 3 || title.trim().length > 150) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "Başlık 3–150 karakter arasında olmalıdır."
        );
      }
    }

    if (content !== undefined) {
      if (typeof content !== "string" || content.trim().length < 10 || content.trim().length > 5000) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "İçerik 10–5000 karakter arasında olmalıdır."
        );
      }
    }

    const updateData: Record<string, unknown> = {
      // Düzenleme sonrası tekrar onaya gider
      status: "pending_approval",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      editedAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    if (title !== undefined) updateData.title = title.trim();
    if (content !== undefined) updateData.content = content.trim();

    await postRef.update(updateData);

    functions.logger.info(`Gönderi güncellendi ve onaya gönderildi: ${postId} (uid: ${uid})`);

    return { success: true, newStatus: "pending_approval" };
  });