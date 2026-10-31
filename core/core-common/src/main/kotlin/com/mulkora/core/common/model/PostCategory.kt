package com.mulkora.core.common.model

/**
 * Gönderi kategorileri.
 * CLAUDE.md Bölüm 7: Gönderi Kategorileri
 * docs/02-functional-requirements.md ve docs/12-firestore-document-schemas.md ile eşleşir.
 *
 * NOT: Bu kategoriler Firestore'daki posts koleksiyonunun "category" alanı ile eşleşir.
 */
enum class PostCategory(val value: String, val displayName: String) {
    RENT_INCREASE("rent_increase", "Kira Artışı"),
    DEPOSIT("deposit", "Depozito"),
    DUES("dues", "Aidat"),
    HOME_MAINTENANCE("home_maintenance", "Ev Bakımı"),
    RENOVATION("renovation", "Tadilat"),
    NOISE("noise", "Gürültü"),
    NEIGHBORHOOD("neighborhood", "Komşuluk"),
    EVICTION("eviction", "Tahliye"),
    LEASE_CONTRACT("lease_contract", "Kira Sözleşmesi"),
    COMMUNICATION("communication", "Ev Sahibi ve Kiracı İletişimi"),
    BILLS("bills", "Faturalar"),
    MOVING("moving", "Taşınma"),
    DAMAGE_MAINTENANCE("damage_maintenance", "Hasar ve Bakım"),
    GENERAL("general", "Genel");

    companion object {
        fun fromValue(value: String): PostCategory =
            entries.find { it.value == value } ?: GENERAL
    }
}