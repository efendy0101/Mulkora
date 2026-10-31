package com.mulkora.feature.profile.domain.model

/**
 * Engellenen kullanıcı domain modeli.
 * CLAUDE.md §8: Kullanıcı engelleme, blockUser Cloud Function üzerinden yapılır.
 */
data class BlockedUser(
    val uid: String,
    val username: String,
    val displayName: String,
    val photoUrl: String?,
    val blockedAt: Long, // epoch millis
)