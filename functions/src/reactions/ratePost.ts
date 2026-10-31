import * as functions from "firebase-functions/v1";
import * as admin from "../firebaseAdmin";

/**
 * Gönderi puanlama Cloud Function.
 * CLAUDE.md §10: Puanlama — Her kullanıcı bir gönderiye yalnızca bir kez 1–5 yıldız verebilir.
 *
 * Kurallar:
 * - Yalnızca onaylı gönderiler puanlanabilir.
 * - Kendi gönderisini puanlayamazsın.
 * - Aynı kullanıcı aynı gönderiyi yalnızca bir kez puanlayabilir
 *   (mevcut puanı değiştirmek de desteklenir — CLAUDE'da belirtilmemiş, prudent default).
 * - likeCount/averageRating transaction ile atomik güncellenir.
 */
export const ratePost = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Puanlamak için giriş yapmanız gerekiyor."
      );
    }

    const uid = context.auth.uid;
    const { postId, rating } = data as { postId?: string; rating?: number };

    // Doğrulama
    if (!postId || typeof postId !== "string") {
      throw new functions.https.HttpsError("invalid-argument", "postId gerekli.");
    }
    if (typeof rating !== "number" || !Number.isInteger(rating) || rating < 1 || rating > 5) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Puan 1 ile 5 arasında tam sayı olmalıdır."
      );
    }

    const db = admin.firestore();
    const postRef = db.collection("posts").doc(postId);
    // ratingId = "{postId}_{uid}" — kullanıcı başına tek puan
    const ratingId = `${postId}_${uid}`;
    const ratingRef = db.collection("postRatings").doc(ratingId);

    await db.runTransaction(async (tx) => {
      const [postSnap, ratingSnap] = await Promise.all([
        tx.get(postRef),
        tx.get(ratingRef),
      ]);

      // Gönderi mevcut mu?
      if (!postSnap.exists) {
        throw new functions.https.HttpsError("not-found", "Gönderi bulunamadı.");
      }

      const postData = postSnap.data()!;

      // Yalnızca onaylı gönderiler puanlanabilir
      if (postData.status !== "approved") {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "Yalnızca onaylı gönderiler puanlanabilir."
        );
      }

      // Kendi gönderisini puanlayamazsın
      if (postData.authorUid === uid) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "Kendi gönderinizi puanlayamazsınız."
        );
      }

      const currentRatingCount = (postData.ratingCount as number) ?? 0;
      const currentAverageRating = (postData.averageRating as number) ?? 0;

      if (ratingSnap.exists) {
        // Kullanıcı daha önce puan vermiş — güncelle
        const previousRating = (ratingSnap.data()!.rating as number) ?? 0;

        if (previousRating === rating) {
          // Aynı puan — işlem yok
          return;
        }

        // Ortalamayı yeniden hesapla
        const totalRating = currentAverageRating * currentRatingCount;
        const newTotalRating = totalRating - previousRating + rating;
        const newAverage = newTotalRating / currentRatingCount;

        tx.update(postRef, {
          averageRating: newAverage,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        tx.update(ratingRef, {
          rating,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      } else {
        // İlk puan
        const newRatingCount = currentRatingCount + 1;
        const totalRating = currentAverageRating * currentRatingCount;
        const newAverage = (totalRating + rating) / newRatingCount;

        tx.update(postRef, {
          averageRating: newAverage,
          ratingCount: admin.firestore.FieldValue.increment(1),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        tx.set(ratingRef, {
          postId,
          uid,
          rating,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }
    });

    return { success: true };
  });