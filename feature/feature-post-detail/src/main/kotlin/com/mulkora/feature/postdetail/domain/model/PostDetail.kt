package com.mulkora.feature.postdetail.domain.model

import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.model.PostStatus
import com.mulkora.core.common.model.PostType
import com.mulkora.core.common.model.UserRole

/**
 * Gönderi detay ekranı için domain modeli.
 *
 * SCR-020 Gönderi detayı ekranında kullanılır.
 * Yazar bilgileri, etkileşim sayaçları ve mevcut kullanıcının oy durumunu içerir.
 *
 * CLAUDE.md §6: Gönderi kartında bulunması gereken tüm alanları kapsar.
 * CLAUDE.md §10: Beğeni/eksi oy durumu istemciden değiştirilemez,
 *   yalnızca Cloud Functions yanıtı sonrası güncellenir.
 */
data class PostDetail(
    val id: String,
    val authorId: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorPhotoUrl: String?,
    val authorRole: UserRole,
    val title: String,
    val body: String,
    val imageUrl: String?,
    val category: PostCategory,
    val type: PostType,
    val city: String,
    val isAnonymous: Boolean,
    val commentsAllowed: Boolean,
    val status: PostStatus,
    val likeCount: Int,
    val dislikeCount: Int,
    val commentCount: Int,
    val averageRating: Double,
    val ratingCount: Int,
    /** Mevcut kullanıcının bu gönderiye verdiği oy (1 = beğeni, -1 = eksi oy, 0 = oy yok) */
    val currentUserVote: Int,
    /** Mevcut kullanıcının bu gönderiye verdiği puan (null = puan verilmemiş) */
    val currentUserRating: Int?,
    val isSaved: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)