package com.mulkora.feature.reactions.domain.model

/**
 * Bir gönderiye ait kullanıcı reaksiyonunun domain modeli.
 *
 * CLAUDE.md §10: Sayaçlar doğrudan istemci tarafından değiştirilemez.
 * Tepki işlemleri Cloud Functions (reactToPost) üzerinden gerçekleştirilir.
 *
 * @param postId Reaksiyon verilen gönderi ID'si
 * @param userReaction Oturum açan kullanıcının mevcut reaksiyonu (null = yok)
 * @param likeCount Toplam beğeni sayısı
 * @param dislikeCount Toplam eksi oy sayısı
 * @param userRating Kullanıcının verdiği puan (null = puan verilmemiş, 1–5)
 * @param averageRating Gönderi ortalama puanı
 * @param ratingCount Puan veren kullanıcı sayısı
 */
data class PostReaction(
    val postId: String,
    val userReaction: ReactionType? = null,
    val likeCount: Int = 0,
    val dislikeCount: Int = 0,
    val userRating: Int? = null,
    val averageRating: Float = 0f,
    val ratingCount: Int = 0,
)