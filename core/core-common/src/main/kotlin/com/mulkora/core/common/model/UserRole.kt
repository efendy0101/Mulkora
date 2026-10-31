package com.mulkora.core.common.model

/**
 * Mülkora kullanıcı rolleri.
 * CLAUDE.md Bölüm 3: Kullanıcı Rolleri
 *
 * Firebase Custom Claims'deki "role" alanı ile eşleşir.
 * Firestore'daki users/{uid}.role alanı ile eşleşir.
 */
enum class UserRole(val value: String, val displayName: String) {
    TENANT("tenant", "Kiracı"),
    LANDLORD("landlord", "Ev Sahibi"),
    BOTH("both", "Kiracı & Ev Sahibi"),
    MODERATOR("moderator", "Moderatör"),
    ADMIN("admin", "Yönetici");

    companion object {
        fun fromValue(value: String): UserRole =
            entries.find { it.value == value } ?: TENANT
    }
}

/**
 * Kullanıcının içerik oluşturma yetkisi var mı?
 * Sadece tenant, landlord ve both rolleri gönderi oluşturabilir.
 */
fun UserRole.canCreateContent(): Boolean =
    this == UserRole.TENANT || this == UserRole.LANDLORD || this == UserRole.BOTH

/**
 * Kullanıcı moderasyon işlemi yapabilir mi?
 */
fun UserRole.canModerate(): Boolean =
    this == UserRole.MODERATOR || this == UserRole.ADMIN

/**
 * Kullanıcı yönetici mi?
 */
fun UserRole.isAdmin(): Boolean = this == UserRole.ADMIN