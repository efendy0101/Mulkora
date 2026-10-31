package com.mulkora.feature.notifications.domain.model

import com.google.firebase.Timestamp

/**
 * Bildirim domain modeli.
 * CLAUDE.md Bölüm 22: Bildirimler
 * docs/19-notification-system.md
 *
 * Firestore: users/{uid}/notifications/{notifId}
 * Bildirimler yalnızca Cloud Functions tarafından oluşturulur.
 */
data class AppNotification(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val deepLink: String?,    // Navigasyon için deep link
    val referenceId: String?, // İlgili belge ID (postId, referenceId, vb.)
    val fromUid: String?,     // Bildirimi tetikleyen kullanıcı
    val fromDisplayName: String?,
    val fromPhotoUrl: String?,
    val createdAt: Timestamp?
)

/**
 * Bildirim türleri.
 * docs/19-notification-system.md ile eşleşir.
 */
enum class NotificationType(val value: String) {
    // Gönderi bildirimleri
    POST_LIKE("post_like"),
    POST_DISLIKE("post_dislike"),
    POST_COMMENT("post_comment"),
    POST_APPROVED("post_approved"),
    POST_REJECTED("post_rejected"),
    POST_EDIT_REQUESTED("post_edit_requested"),

    // Referans bildirimleri
    REFERENCE_REQUEST("reference_request"),
    REFERENCE_ACCEPTED("reference_accepted"),
    REFERENCE_REJECTED("reference_rejected"),
    REFERENCE_APPROVED("reference_approved"),
    REFERENCE_RESPONSE("reference_response"),
    REFERENCE_DISPUTED("reference_disputed"),

    // Yorum bildirimleri
    COMMENT_LIKE("comment_like"),
    COMMENT_REPLY("comment_reply"),

    // Sistem bildirimleri
    SYSTEM("system"),
    WARNING("warning"),     // Kullanıcı uyarısı
    ACCOUNT_BANNED("account_banned");

    companion object {
        fun fromValue(value: String): NotificationType =
            entries.find { it.value == value } ?: SYSTEM
    }
}