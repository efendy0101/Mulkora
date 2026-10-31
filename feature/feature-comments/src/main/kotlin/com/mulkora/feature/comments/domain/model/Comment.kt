package com.mulkora.feature.comments.domain.model

import com.google.firebase.Timestamp
import com.mulkora.core.common.model.UserRole

/**
 * Yorum durum enum'u.
 * CLAUDE.md §9: Yorum yaşam döngüsü.
 */
enum class CommentStatus(val value: String) {
    ACTIVE("active"),       // Normal, görünür yorum
    HIDDEN("hidden"),       // Moderatör tarafından gizlendi
    DELETED("deleted");     // Silindi — içerik gösterilmez

    companion object {
        fun fromValue(value: String): CommentStatus =
            entries.find { it.value == value } ?: ACTIVE
    }
}

/**
 * Yorum domain modeli — feature-comments modülü.
 * CLAUDE.md §9: Yorum ve Cevap Sistemi.
 *
 * Firestore yolu: posts/{postId}/comments/{commentId}
 * Cevaplar aynı alt koleksiyonda parentId ile bağlanır.
 */
data class Comment(
    val id: String = "",
    val postId: String = "",
    val parentId: String? = null,            // null → üst düzey yorum; dolu → cevap
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorDisplayName: String = "",
    val authorPhotoUrl: String? = null,
    val authorRole: UserRole = UserRole.TENANT,
    val content: String = "",
    val likeCount: Int = 0,
    val replyCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val isCurrentUserAuthor: Boolean = false,
    val isHidden: Boolean = false,           // Moderatör tarafından gizlendi
    val status: CommentStatus = CommentStatus.ACTIVE,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)
