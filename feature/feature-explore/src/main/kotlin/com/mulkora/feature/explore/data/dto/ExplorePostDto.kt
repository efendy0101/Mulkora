package com.mulkora.feature.explore.data.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.model.PostType
import com.mulkora.feature.explore.domain.model.ExplorePost

/**
 * Firestore'dan gelen ham gönderi verisi için DTO.
 *
 * docs/12-firestore-document-schemas.md
 * CLAUDE.md §33: Serileştirme yalnızca data katmanında yapılır.
 */
data class ExplorePostDto(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val type: String = "",
    val city: String? = null,
    @get:PropertyName("authorDisplayName")
    @set:PropertyName("authorDisplayName")
    var authorDisplayName: String = "",
    @get:PropertyName("authorPhotoUrl")
    @set:PropertyName("authorPhotoUrl")
    var authorPhotoUrl: String? = null,
    @get:PropertyName("likeCount")
    @set:PropertyName("likeCount")
    var likeCount: Int = 0,
    @get:PropertyName("commentCount")
    @set:PropertyName("commentCount")
    var commentCount: Int = 0,
    @get:PropertyName("averageRating")
    @set:PropertyName("averageRating")
    var averageRating: Double = 0.0,
    @get:PropertyName("thumbnailUrl")
    @set:PropertyName("thumbnailUrl")
    var thumbnailUrl: String? = null,
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null,
) {
    fun toDomain(): ExplorePost = ExplorePost(
        id = id,
        title = title,
        category = PostCategory.fromValue(category),
        type = PostType.fromValue(type),
        city = city,
        authorDisplayName = authorDisplayName,
        authorPhotoUrl = authorPhotoUrl,
        likeCount = likeCount,
        commentCount = commentCount,
        averageRating = averageRating,
        thumbnailUrl = thumbnailUrl,
        createdAt = createdAt,
    )
}