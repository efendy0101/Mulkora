package com.mulkora.feature.reference.domain.model

import com.google.firebase.Timestamp
import com.mulkora.core.common.model.ReferenceStatus

/**
 * Referans domain modeli.
 * CLAUDE.md Bölüm 9: Referans Sistemi
 * docs/16-reference-system-state-machine.md
 *
 * Referans durumu state machine (tam akış):
 * invite_sent → awaiting_confirmation → not_recognized  [SON]
 *                                     → preparing → pending_approval → approved
 *                                                                     → rejected [SON]
 * approved → appealed → under_review → hidden / deleted
 *          → hidden  → approved / deleted
 *          → deleted [SON]
 */
data class Reference(
    val id: String,
    val fromUid: String,          // Referansı veren
    val toUid: String,            // Referans verilen
    val fromUsername: String,
    val toUsername: String,
    val fromDisplayName: String,
    val toDisplayName: String,
    val fromPhotoUrl: String?,
    val toPhotoUrl: String?,
    val referenceType: ReferenceType,
    val rating: Int,              // 1-5 yıldız
    val content: String,          // Referans metni
    val status: ReferenceStatus,
    val relationshipType: RelationshipType,
    val propertyAddress: String?, // İsteğe bağlı mülk adresi
    val startDate: String?,       // Kiralama başlangıç tarihi (ISO 8601 formatı)
    val endDate: String?,         // Kiralama bitiş tarihi (ISO 8601 formatı)
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?,
    val response: ReferenceResponse? = null  // Yanıt (varsa)
)

/**
 * Referans türü.
 */
enum class ReferenceType(val value: String, val displayName: String) {
    TENANT_TO_LANDLORD("tenant_to_landlord", "Kiracıdan Ev Sahibine"),
    LANDLORD_TO_TENANT("landlord_to_tenant", "Ev Sahibinden Kiracıya");

    companion object {
        fun fromValue(value: String): ReferenceType =
            entries.find { it.value == value } ?: TENANT_TO_LANDLORD
    }
}

/**
 * İlişki türü.
 */
enum class RelationshipType(val value: String, val displayName: String) {
    CURRENT_TENANT("current_tenant", "Mevcut Kiracı"),
    FORMER_TENANT("former_tenant", "Eski Kiracı"),
    CURRENT_LANDLORD("current_landlord", "Mevcut Ev Sahibi"),
    FORMER_LANDLORD("former_landlord", "Eski Ev Sahibi");

    companion object {
        fun fromValue(value: String): RelationshipType =
            entries.find { it.value == value } ?: FORMER_TENANT
    }
}

/**
 * Referans yanıtı.
 * Referans verilen kişinin yanıtı.
 */
data class ReferenceResponse(
    val id: String,
    val respondentUid: String,
    val content: String,
    val status: String,  // visible | hidden
    val createdAt: Timestamp?
)