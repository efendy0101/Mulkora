import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

const db = admin.firestore();

/**
 * Gönderi görüntülenme sayacını güvenli şekilde artırır.
 *
 * CLAUDE.md §26: Sayaçlar istemciden değiştirilemez — CF kullanılır.
 * Android tarafında FeedRepositoryImpl.incrementViewCount() bu fonksiyonu çağırır.
 *
 * İdempotency: Aynı kullanıcı aynı oturumda birden fazla kez görüntülese bile
 * sayaç yalnızca bir kez artırılır (viewedBy alt koleksiyonu ile kontrol edilir).
 *
 * Hata: Sayaç artırma başarısız olsa bile istemci tarafında sessizce yutulur
 * (kritik bir işlem değildir).
 */
export const incrementViewCount = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      // View count için auth zorunlu değil — anonim artırma kabul edilmez
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Görüntülenme sayısı için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const { postId } = data as { postId?: string };

    if (!postId || typeof postId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir postId gereklidir."
      );
    }

    try {
      const postRef = db.collection("posts").doc(postId);
      const viewRef = postRef.collection("viewedBy").doc(uid);

      // viewRef kontrolü transaction içinde — race condition önlenir
      let counted = false;
      await db.runTransaction(async (tx) => {
        // Her iki belgeyi transaction içinde oku
        const [viewSnap, postSnap] = await Promise.all([
          tx.get(viewRef),
          tx.get(postRef),
        ]);

        if (!postSnap.exists) return;

        // Aynı kullanıcı zaten saydırdıysa idempotent çık
        if (viewSnap.exists) return;

        // Atomik: viewedBy yaz + viewCount artır
        tx.set(viewRef, {
          viewedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        tx.update(postRef, {
          viewCount: admin.firestore.FieldValue.increment(1),
        });
        counted = true;
      });

      return { success: true, counted };
    } catch (error) {
      // Görüntülenme sayacı kritik değil — loglama yapıp başarı dön
      functions.logger.warn("incrementViewCount hatası", { postId, uid, error });
      return { success: false, counted: false };
    }
  });