package com.mulkora.feature.reactions.domain.model

/**
 * Bir gönderiye verilen tepkiyi temsil eder.
 *
 * CLAUDE.md §10: Beğeni/eksi oy/puan sisteminin iş kuralları.
 * - Kullanıcı aynı anda hem beğeni hem eksi oy veremez.
 * - Oylar yalnızca Cloud Functions (togglePostReaction) üzerinden değiştirilebilir.
 * - Sayaçlar doğrudan Firestore'dan yazılamaz.
 */
data class Reaction(
    val id: String,
    val postId: String,
    val userId: String,
    /** 1 = beğeni, -1 = eksi oy */
    val vote: Int,
    val createdAt: Long,
)

/**
 * Bir gönderiye verilen puanı temsil eder.
 *
 * CLAUDE.md §10: Her kullanıcı bir gönderiye yalnızca bir kez 1–5 yıldız puan verebilir.
 */
data class PostRating(
    val id: String,
    val postId: String,
    val userId: String,
    /** 1–5 arası puan */
    val rating: Int,
    val createdAt: Long,
)