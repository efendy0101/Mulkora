package com.mulkora.feature.comments.domain.repository

import com.mulkora.core.common.result.Result
import com.mulkora.feature.comments.domain.model.Comment
import kotlinx.coroutines.flow.Flow

/**
 * Yorum repository arayüzü.
 * CLAUDE.md §9: Yorum ve Cevap Sistemi.
 *
 * Kurallar:
 * - Yorumlar Firestore alt koleksiyonu kullanır: posts/{postId}/comments
 * - Yorum ekleme Cloud Function üzerinden yapılır (içerik filtresi için)
 * - Sayaçlar (likeCount, replyCount) istemciden değiştirilemez — CF kullanılır
 * - Silinmiş yorumlar içerik gizlenir ama kayıt tutulur (replyCount korunur)
 */
interface CommentsRepository {

    /**
     * Gönderi yorumlarını gerçek zamanlı dinler.
     * Gizlenen yorumlar (isHidden=true) sadece moderatörlere gösterilir.
     */
    fun observeComments(postId: String): Flow<Result<List<Comment>>>

    /**
     * Yeni yorum ekler veya cevap verir.
     * [parentId] null ise üst düzey yorum, dolu ise cevap.
     */
    suspend fun addComment(
        postId: String,
        body: String,
        parentId: String? = null,
    ): Result<Unit>

    /**
     * Yorumu siler. Sadece yorum sahibi veya moderatör silebilir.
     * CLAUDE.md §26: Silme işlemi Cloud Function'a devredilir.
     */
    suspend fun deleteComment(postId: String, commentId: String): Result<Unit>

    /**
     * Yorum beğeni durumunu değiştirir (toggle).
     * CLAUDE.md §26: Sayaç değişikliği CF üzerinden yapılır.
     */
    suspend fun toggleCommentLike(postId: String, commentId: String): Result<Unit>
}