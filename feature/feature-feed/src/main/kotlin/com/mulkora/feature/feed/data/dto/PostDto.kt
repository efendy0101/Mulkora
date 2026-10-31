package com.mulkora.feature.feed.data.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.model.PostStatus
import com.mulkora.core.common.model.PostType
import com.mulkora.core.common.model.UserRole
import com.mulkora.feature.feed.domain.model.Post

/**
 * Firestore veri transfer nesnesi — Gönderi.
 * docs/12-firestore-document-schemas.md posts koleksiyonu ile eşleşir.
 *
 * Firestore'a okunurken ve yazılırken kullanılır.
 * Domain modeline (Post) dönüştürülür.
 */
data class PostDto(
    @DocumentId
    val id: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorDisplayName: String = "",
    val authorPhotoUrl: String? = null,
    val authorRole: String = "tenant",
    val title: String = "",
    val content: String = "",
    val postType: String = "question",
    val category: String = "general",
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    val status: String = "pending",
    val likeCount: Int = 0,
    val dislikeCount: Int = 0,
    val commentCount: Int = 0,
    val averageRating: Double = 0.0,
    val ratingCount: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    /**
     * Firestore no-argument constructor için boş constructor gereklidir.
     */
    constructor() : this(id = "")

    /**
     * DTO'yu domain modeline dönüştürür.
     */
    fun toPost(): Post = Post(
        id = id,
        authorUid = authorUid,
        authorUsername = authorUsername,
        authorDisplayName = authorDisplayName,
        authorPhotoUrl = authorPhotoUrl,
        authorRole = UserRole.fromValue(authorRole),
        title = title,
        content = content,
        postType = PostType.fromValue(postType),
        category = PostCategory.fromValue(category),
        tags = tags,
        imageUrl = imageUrl,
        status = PostStatus.fromValue(status),
        likeCount = likeCount,
        dislikeCount = dislikeCount,
        commentCount = commentCount,
        averageRating = averageRating,
        ratingCount = ratingCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}