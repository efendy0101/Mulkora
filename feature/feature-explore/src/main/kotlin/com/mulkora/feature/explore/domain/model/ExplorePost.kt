package com.mulkora.feature.explore.domain.model

import com.google.firebase.Timestamp
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.model.PostType

/**
 * Keşfet ekranları için özet gönderi modeli.
 *
 * CLAUDE.md §33: Domain model; Firestore dökümanından map edilir.
 */
data class ExplorePost(
    val id: String,
    val title: String,
    val category: PostCategory,
    val type: PostType,
    val city: String?,
    val authorDisplayName: String,
    val authorPhotoUrl: String?,
    val likeCount: Int,
    val commentCount: Int,
    val averageRating: Double,
    val thumbnailUrl: String?,
    val createdAt: Timestamp?
)