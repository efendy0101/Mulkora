import * as functions from "firebase-functions/v1";
import * as admin from "../firebaseAdmin";
import { checkContentFilter } from "../moderation/contentFilter";

/**
 * Yorum ekleme Cloud Function.
 * CLAUDE.md Bölüm 9: Yorum ve Cevap Sistemi.
 *
 * Güvenlik kuralları:
 * - Kimlik doğrulanmış kullanıcılar yorum yapabilir.
 * - İçerik filtresi uygulanır; yüksek riskli yorumlar moderasyon kuyruğuna gider.
 * - Sayaçlar (commentCount, replyCount) atomik olarak güncellenir.
 */
export const addComment = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Yorum yapabilmek için giriş yapmalısınız."
      );
    }

    const uid = context.auth.uid;
    const { postId, content, parentId } = data as {
      postId?: string;
      content?: string;
      parentId?: string;
    };

    if (!postId || typeof postId !== "string") {
      throw new functions.https.HttpsError("invalid-argument", "postId geçersiz.");
    }
    if (!content || typeof content !== "string" || content.trim().length === 0) {
      throw new functions.https.HttpsError("invalid-argument", "Yorum içeriği boş olamaz.");
    }
    if (content.trim().length > 2000) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Yorum en fazla 2000 karakter olabilir."
      );
    }

    const db = admin.firestore();

    // Gönderi var mı ve onaylanmış mı?
    // CLAUDE.md §17: Onaylı durum "approved" string'i ile temsil edilir.
    const postDoc = await db.collection("posts").doc(postId).get();
    if (!postDoc.exists || postDoc.data()?.status !== "approved") {
      throw new functions.https.HttpsError("not-found", "Gönderi bulunamadı veya henüz onaylanmamış.");
    }

    // Kullanıcı engellendi mi?
    const userDoc = await db.collection("users").doc(uid).get();
    const userData = userDoc.data();
    if (!userData || userData.isBanned) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Hesabınız askıya alınmış."
      );
    }

    // İçerik filtresi
    const filterResult = await checkContentFilter(content.trim());
    const isHidden = filterResult.riskLevel === "HIGH";
    const needsModeration = filterResult.riskLevel !== "LOW";

    const commentData: Record<string, unknown> = {
      postId,
      authorUid: uid,
      authorUsername: userData.username ?? "",
      authorDisplayName: userData.displayName ?? "",
      authorPhotoUrl: userData.photoUrl ?? null,
      authorRole: userData.role ?? "TENANT",
      content: content.trim(),
      likeCount: 0,
      replyCount: 0,
      likedBy: [],
      isHidden,
      needsModeration,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    if (parentId && typeof parentId === "string") {
      // Cevap: parentId belirli bir yoruma ait mi kontrol et
      const parentDoc = await db
        .collection("posts")
        .doc(postId)
        .collection("comments")
        .doc(parentId)
        .get();
      if (!parentDoc.exists) {
        throw new functions.https.HttpsError("not-found", "Cevap verilecek yorum bulunamadı.");
      }
      commentData["parentId"] = parentId;
    } else {
      commentData["parentId"] = null;
    }

    const batch = db.batch();

    // Yorum belgesi
    const commentRef = db
      .collection("posts")
      .doc(postId)
      .collection("comments")
      .doc();
    batch.set(commentRef, commentData);

    // Üst düzey yorum: postun commentCount sayacını artır
    if (!parentId) {
      batch.update(db.collection("posts").doc(postId), {
        commentCount: admin.firestore.FieldValue.increment(1),
      });
    } else {
      // Cevap: parent yorumun replyCount'unu artır
      batch.update(
        db
          .collection("posts")
          .doc(postId)
          .collection("comments")
          .doc(parentId),
        { replyCount: admin.firestore.FieldValue.increment(1) }
      );
    }

    // Moderasyon kuyruğu
    if (needsModeration) {
      const modRef = db.collection("moderationQueue").doc();
      batch.set(modRef, {
        type: "COMMENT",
        postId,
        commentId: commentRef.id,
        authorUid: uid,
        content: content.trim(),
        riskLevel: filterResult.riskLevel,
        matchedTerms: filterResult.matchedTerms ?? [],
        status: "PENDING",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    // Gönderi sahibine bildirim (kendi yorumu ise gönderme)
    const postOwnerId = postDoc.data()?.authorUid as string | undefined;
    if (postOwnerId && postOwnerId !== uid && !parentId) {
      const notifRef = db
        .collection("users")
        .doc(postOwnerId)
        .collection("notifications")
        .doc();
      batch.set(notifRef, {
        type: "NEW_COMMENT",
        postId,
        commentId: commentRef.id,
        fromUid: uid,
        fromUsername: userData.username ?? "",
        isRead: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    await batch.commit();

    return {
      success: true,
      commentId: commentRef.id,
      isHidden,
      message: isHidden
        ? "Yorumunuz incelemeye alındı."
        : "Yorum başarıyla eklendi.",
    };
  });