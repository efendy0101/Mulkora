package com.mulkora.feature.feed.domain.model

import com.google.firebase.Timestamp
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.model.PostStatus
import com.mulkora.core.common.model.PostType
import com.mulkora.core.common.model.UserRole

/**
 * Gönderi domain modeli.
 * CLAUDE.md Bölüm 7: Gönderi Oluşturma ve docs/12-firestore-document-schemas.md
 *
 * Bu model Firestore PostDto'dan dönüştürülür.
 * İş mantığı açısından zengin bir model.
 */
data class Post(
    val id: String,
    val authorUid: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorPhotoUrl: String?,
    val authorRole: UserRole,
    val title: String,
    val content: String,
    val postType: PostType,
    val category: PostCategory,
    val tags: List<String>,
    val imageUrl: String?,
    val status: PostStatus,
    val likeCount: Int,
    val dislikeCount: Int,
    val commentCount: Int,
    val averageRating: Double,
    val ratingCount: Int,
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?,

    // Mevcut kullanıcının etkileşim durumu (sunucu tarafından hesaplanmaz)
    // Repository katmanında postReactions koleksiyonundan ayrıca yüklenir
    val currentUserReaction: ReactionType? = null,
    val currentUserRating: Int? = null,
    val isCurrentUserAuthor: Boolean = false,
    val isSaved: Boolean = false
)

/**
 * Gönderi tepki türü.
 */
enum class ReactionType(val value: String) {
    LIKE("like"),
    DISLIKE("dislike");

    companion object {
        fun fromValue(value: String?): ReactionType? =
            entries.find { it.value == value }
    }
}

/**
 * Feed öğesi — Gönderi veya Reklam.
 * CLAUDE.md Bölüm 30: Reklam sistemi.
 *
 * Feed listesi hem gerçek gönderileri hem de native reklamları içerebilir.
 */
sealed interface FeedItem {
    data class PostItem(val post: Post) : FeedItem
    data object NativeAdItem : FeedItem
}