package com.mulkora.feature.reactions.domain.model

/**
 * Bir gönderiye kullanıcının verebileceği tepki türü.
 *
 * CLAUDE.md §10: Bir kullanıcı aynı gönderiye aynı anda hem beğeni hem eksi oy veremez.
 * Kullanıcı oyunu kaldırabilir, beğeniyi eksi oya veya eksi oyu beğeniye çevirebilir.
 */
enum class ReactionType {
    LIKE,
    DISLIKE,
}