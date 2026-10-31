import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";

const db = admin.firestore();

/**
 * Gönderi tepkisi ekler veya kaldırır.
 * CLAUDE.md §9: Beğeni/eksi oy sayaçları atomik transaction ile güncellenir.
 *
 * Atomic increment/decrement kullanır.
 * Kullanıcı aynı tepkiyi tekrar verirse toggle (kaldırır).
 * Farklı tepki verirse öncekini kaldırıp yenisini ekler.
 *
 * FIX: reactionSnap transaction DIŞINDA okunmamalıdır —
 * tüm okumalar transaction içine alındı (serializability garantisi).
 */
export const togglePostReaction = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const { postId, reactionType } = data;

    if (!postId || typeof postId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir postId gereklidir."
      );
    }

    if (!reactionType || !["like", "dislike"].includes(reactionType)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        'Geçerli reactionType: "like" veya "dislike"'
      );
    }

    const postRef = db.collection("posts").doc(postId);
    const reactionRef = db
      .collection("posts")
      .doc(postId)
      .collection("reactions")
      .doc(uid);

    const increment = admin.firestore.FieldValue.increment;

    return db.runTransaction(async (transaction) => {
      // Tüm okumalar transaction başında — Firestore serializability garantisi.
      const [postSnap, reactionSnap] = await Promise.all([
        transaction.get(postRef),
        transaction.get(reactionRef),
      ]);

      if (!postSnap.exists) {
        throw new functions.https.HttpsError("not-found", "Gönderi bulunamadı.");
      }

      const post = postSnap.data()!;

      // Yalnızca approved gönderilere tepki verilebilir
      if (post.status !== "approved") {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "Yalnızca onaylanmış gönderilere tepki verilebilir."
        );
      }

      // Kendi gönderisine tepki veremez
      const authorUid = post.authorUid ?? post.authorId;
      if (authorUid === uid) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "Kendi gönderinize tepki veremezsiniz."
        );
      }

      if (!reactionSnap.exists) {
        // Hiç tepki yok → ekle
        transaction.set(reactionRef, {
          uid,
          reactionType,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        if (reactionType === "like") {
          transaction.update(postRef, { likeCount: increment(1) });
        } else {
          transaction.update(postRef, { dislikeCount: increment(1) });
        }

        return { action: "added", reactionType };
      }

      const existingReaction = reactionSnap.data()!;

      if (existingReaction.reactionType === reactionType) {
        // Aynı tepki → kaldır (toggle)
        transaction.delete(reactionRef);

        if (reactionType === "like") {
          transaction.update(postRef, { likeCount: increment(-1) });
        } else {
          transaction.update(postRef, { dislikeCount: increment(-1) });
        }

        return { action: "removed", reactionType };
      } else {
        // Farklı tepki → değiştir
        transaction.update(reactionRef, {
          reactionType,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        if (reactionType === "like") {
          transaction.update(postRef, {
            likeCount: increment(1),
            dislikeCount: increment(-1),
          });
        } else {
          transaction.update(postRef, {
            likeCount: increment(-1),
            dislikeCount: increment(1),
          });
        }

        return { action: "changed", reactionType };
      }
    });
  });