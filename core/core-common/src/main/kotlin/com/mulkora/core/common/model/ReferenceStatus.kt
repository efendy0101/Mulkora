package com.mulkora.core.common.model

/**
 * Referans sistemi durumları — Durum makinesi.
 * CLAUDE.md Bölüm 13 ve docs/16-reference-system-state-machine.md
 *
 * Firestore'daki references/{referenceId}.status alanı ile eşleşir.
 *
 * State machine akışı:
 * invite_sent → awaiting_confirmation → not_recognized (son)
 *                                     → preparing → pending_approval → approved
 *                                                                    → rejected (son)
 * approved → appealed → under_review → hidden / deleted
 *          → hidden  → approved / deleted
 *          → deleted (son)
 */
enum class ReferenceStatus(val value: String) {
    INVITE_SENT("invite_sent"),                          // Davet gönderildi
    AWAITING_CONFIRMATION("awaiting_confirmation"),      // Tanıma onayı bekleniyor
    NOT_RECOGNIZED("not_recognized"),                    // Tanınmadı — süreç durdu
    PREPARING("preparing"),                              // Referans yazılıyor
    PENDING_APPROVAL("pending_approval"),                // Moderatör onayı bekliyor
    APPROVED("approved"),                                // Onaylandı
    REJECTED("rejected"),                                // Reddedildi
    APPEALED("appealed"),                                // İtiraz edildi
    UNDER_REVIEW("under_review"),                        // İtiraz incelemede
    HIDDEN("hidden"),                                    // Gizlendi
    DELETED("deleted");                                  // Silindi

    companion object {
        fun fromValue(value: String): ReferenceStatus =
            entries.find { it.value == value } ?: INVITE_SENT
    }
}

/**
 * Referans durum makinesi geçiş kuralları.
 * docs/16-reference-system-state-machine.md ile eşleşir.
 */
object ReferenceStateMachine {
    fun isValidTransition(from: ReferenceStatus, to: ReferenceStatus): Boolean {
        return when (from) {
            ReferenceStatus.INVITE_SENT          -> to == ReferenceStatus.AWAITING_CONFIRMATION
            ReferenceStatus.AWAITING_CONFIRMATION -> to in setOf(
                ReferenceStatus.NOT_RECOGNIZED,
                ReferenceStatus.PREPARING
            )
            ReferenceStatus.NOT_RECOGNIZED       -> false  // Son durum
            ReferenceStatus.PREPARING            -> to == ReferenceStatus.PENDING_APPROVAL
            ReferenceStatus.PENDING_APPROVAL     -> to in setOf(
                ReferenceStatus.APPROVED,
                ReferenceStatus.REJECTED
            )
            ReferenceStatus.APPROVED             -> to in setOf(
                ReferenceStatus.APPEALED,
                ReferenceStatus.HIDDEN,
                ReferenceStatus.DELETED
            )
            ReferenceStatus.REJECTED             -> to == ReferenceStatus.DELETED
            ReferenceStatus.APPEALED             -> to == ReferenceStatus.UNDER_REVIEW
            ReferenceStatus.UNDER_REVIEW         -> to in setOf(
                ReferenceStatus.APPROVED,
                ReferenceStatus.HIDDEN,
                ReferenceStatus.DELETED
            )
            ReferenceStatus.HIDDEN               -> to in setOf(
                ReferenceStatus.APPROVED,
                ReferenceStatus.DELETED
            )
            ReferenceStatus.DELETED              -> false  // Son durum
        }
    }

    /** Bekleyen davet durumları — kullanıcıya tanıma sorusu gösterilecek */
    val pendingInviteStatuses: Set<String> = setOf(
        ReferenceStatus.INVITE_SENT.value,
        ReferenceStatus.AWAITING_CONFIRMATION.value,
    )

    /** Referansın profilde görünür olduğu durumlar */
    val visibleStatuses: Set<String> = setOf(ReferenceStatus.APPROVED.value)
}

/**
 * Referans hedef kullanıcının profilinde görünür mü?
 */
fun ReferenceStatus.isVisibleOnProfile(): Boolean = this == ReferenceStatus.APPROVED