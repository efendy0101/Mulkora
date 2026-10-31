import * as admin from "../firebaseAdmin";
import * as functions from "firebase-functions/v1";
import { analyzeContent } from "../moderation/contentFilter";

const db = admin.firestore();

/**
 * Yorum içeriğini düzenler.
 *
 * CF-COMMENT-002: docs/14-cloud-functions-architecture.md
 * CLAUDE.md §9: Yorum ve Cevap Sistemi
 * docs/22-development-phases.md Faz 5: editComment
 *
 * Kurallar:
 * - Yorum yalnızca kendi yazarı tarafından düzenlenebilir
 * - Moderatör yorumlarını düzenleyemez (yalnızca silebilir)
 * - Maksimum 1000 karakter
 * - Düzenleme sonrası `editedAt` alanı güncellenir
 * - İçerik filtresi çalıştırılır
 */
export const editComment = functions
  .region("europe-west1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Bu işlem için giriş yapılmış olmalıdır."
      );
    }

    const uid = context.auth.uid;
    const { postId, commentId, content } = data;

    if (!postId || typeof postId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir postId gereklidir."
      );
    }

    if (!commentId || typeof commentId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Geçerli bir commentId gereklidir."
      );
    }

    if (!content || typeof content !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Yorum içeriği gereklidir."
      );
    }

    const trimmedContent = content.trim();

    if (trimmedContent.length === 0 || trimmedContent.length > 1000) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Yorum 1–1000 karakter arasında olmalıdır."
      );
    }

    // Yorumu al
    const commentRef = db
      .collection("posts")
      .doc(postId)
      .collection("comments")
      .doc(commentId);

    const commentSnap = await commentRef.get();

    if (!commentSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Yorum bulunamadı.");
    }

    const comment = commentSnap.data()!;

    // Yalnızca yorumun sahibi düzenleyebilir
    if (comment.authorUid !== uid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Yalnızca yorumun sahibi düzenleme yapabilir."
      );
    }

    // Silinmiş yorum düzenlenemez
    if (comment.isDeleted === true) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Silinmiş bir yorum düzenlenemez."
      );
    }

    // İçerik filtresi — contentFilter.ts ile aynı motor (bypass ve kişisel veri koruması dahil)
    const bannedSnap = await db.collection("bannedTerms").limit(500).get();
    // .data().term — bannedTerms belge alanı "term" olarak saklanır (onPostCreated ile aynı yapı)
    const bannedTerms = bannedSnap.docs
      .map((d) => d.data().term as string)
      .filter(Boolean);

    const filterResult = analyzeContent(trimmedContent, bannedTerms);

    if (filterResult.blocked) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        `Yorum uygunsuz içerik barındırıyor: ${filterResult.reasons.join("; ")}`
      );
    }

    const now = admin.firestore.FieldValue.serverTimestamp();

    // MEDIUM risk (flagged): yorum gizlenir ve moderasyon kuyruğuna alınır
    if (filterResult.flagged) {
      await commentRef.update({
        content: trimmedContent,
        isHidden: true,
        needsModeration: true,
        editedAt: now,
        updatedAt: now,
      });

      // Moderasyon kuyruğuna ekle
      await db.collection("moderationQueue").add({
        type: "COMMENT_EDIT",
        postId,
        commentId,
        authorUid: uid,
        content: trimmedContent,
        riskLevel: filterResult.riskLevel,
        reasons: filterResult.reasons,
        status: "PENDING",
        createdAt: now,
      });

      functions.logger.info(
        `Yorum düzenleme moderasyona alındı (${filterResult.riskLevel}): ${commentId}`
      );

      return {
        success: true,
        pendingReview: true,
        message: "Yorumunuz incelemeye alındı ve onaylandıktan sonra yayımlanacak.",
      };
    }

    await commentRef.update({
      content: trimmedContent,
      editedAt: now,
      updatedAt: now,
    });

    functions.logger.info(`Yorum düzenlendi: ${commentId} (uid: ${uid}, post: ${postId})`);

    return { success: true, pendingReview: false };
  });