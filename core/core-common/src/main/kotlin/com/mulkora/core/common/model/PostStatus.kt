package com.mulkora.core.common.model

/**
 * Gönderi durumları — Durum makinesi.
 * CLAUDE.md Bölüm 8 ve docs/17-post-moderation-state-machine.md
 *
 * Firestore'daki posts/{postId}.status alanı ile eşleşir.
 */
enum class PostStatus(val value: String) {
    DRAFT("draft"),                          // Taslak
    PENDING("pending"),                      // İnceleme bekliyor
    FILTERED("filtered"),                    // Otomatik filtreye takıldı
    UNDER_REVIEW("under_review"),            // Yönetici incelemesinde
    EDIT_REQUESTED("edit_requested"),        // Düzenleme bekliyor
    APPROVED("approved"),                    // Onaylandı
    REJECTED("rejected"),                    // Reddedildi
    UNPUBLISHED("unpublished"),              // Yayından kaldırıldı
    DELETED("deleted");                      // Silindi

    companion object {
        fun fromValue(value: String): PostStatus =
            entries.find { it.value == value } ?: PENDING
    }
}

/**
 * Durum makinesi geçiş kuralları.
 * CLOUD.md: Durum geçişleri yalnızca Cloud Functions üzerinden yapılmalıdır.
 */
object PostStateMachine {

    /**
     * Bir durumdan diğerine geçiş izinli mi?
     * Bu kurallar Cloud Functions'da da uygulanır; burada Android tarafı için.
     */
    fun isValidTransition(from: PostStatus, to: PostStatus): Boolean {
        return when (from) {
            PostStatus.DRAFT          -> to == PostStatus.PENDING
            PostStatus.PENDING        -> to in setOf(PostStatus.FILTERED, PostStatus.UNDER_REVIEW, PostStatus.APPROVED, PostStatus.REJECTED)
            PostStatus.FILTERED       -> to in setOf(PostStatus.UNDER_REVIEW, PostStatus.REJECTED)
            PostStatus.UNDER_REVIEW   -> to in setOf(PostStatus.APPROVED, PostStatus.REJECTED, PostStatus.EDIT_REQUESTED)
            PostStatus.EDIT_REQUESTED -> to in setOf(PostStatus.PENDING, PostStatus.DELETED)
            PostStatus.APPROVED       -> to in setOf(PostStatus.UNPUBLISHED, PostStatus.DELETED)
            PostStatus.REJECTED       -> to == PostStatus.DELETED
            PostStatus.UNPUBLISHED    -> to in setOf(PostStatus.APPROVED, PostStatus.DELETED)
            PostStatus.DELETED        -> false  // Silinen gönderi başka duruma geçemez
        }
    }
}

/**
 * Gönderi kullanıcı tarafından görülebilir mi?
 * Yalnızca APPROVED durumundaki gönderiler genel akışta gösterilir.
 */
fun PostStatus.isVisibleToPublic(): Boolean = this == PostStatus.APPROVED

/**
 * Gönderi sahibinin düzenleyebileceği durumlar.
 */
fun PostStatus.isEditableByOwner(): Boolean =
    this == PostStatus.DRAFT || this == PostStatus.EDIT_REQUESTED