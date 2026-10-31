package com.mulkora.feature.profile.domain.model

import com.google.firebase.Timestamp
import com.mulkora.core.common.model.UserRole

/**
 * Kullanıcı profili domain modeli.
 * CLAUDE.md Bölüm 3: Kullanıcı Profili
 * docs/12-firestore-document-schemas.md — users koleksiyonu
 */
data class UserProfile(
    val uid: String,
    val username: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val bio: String?,
    val role: UserRole,
    val isEmailVerified: Boolean,
    val isBlocked: Boolean,
    val adsRemoved: Boolean,
    val postCount: Int,
    val referenceCount: Int,
    val averageReferenceRating: Double,
    val createdAt: Timestamp?,
    val city: String?,
    val links: SocialLinks?
)

/**
 * Sosyal medya bağlantıları.
 */
data class SocialLinks(
    val twitter: String? = null,
    val linkedin: String? = null,
    val instagram: String? = null
)

/**
 * Profil güncelleme isteği.
 * Yalnızca değiştirilebilir alanları içerir.
 * CLAUDE.md: Rol ve satın alma durumu değiştirilemez.
 */
data class UpdateProfileRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val city: String? = null,
    val links: SocialLinks? = null,
    val photoUrl: String? = null  // Storage'a yüklendikten sonra URL
)