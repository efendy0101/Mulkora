package com.mulkora.feature.auth.domain.model

/**
 * Auth domain modeli.
 * Firebase Auth kullanıcısını domain katmanına taşır.
 * CLAUDE.md Bölüm 2: Kimlik Doğrulama
 */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    val isAnonymous: Boolean = false
)

/**
 * Giriş yöntemi.
 */
enum class AuthProvider {
    EMAIL,
    GOOGLE
}