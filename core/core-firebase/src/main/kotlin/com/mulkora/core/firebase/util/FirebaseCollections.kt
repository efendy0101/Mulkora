package com.mulkora.core.firebase.util

/**
 * Firestore koleksiyon ve belge yolu sabitleri.
 * docs/11-firebase-collections.md ile eşleşir.
 *
 * Tüm Firestore yolları bu dosyadan referans alınır.
 * Hardcoded string kullanımı yasaktır.
 */
object FirebaseCollections {

    // --- Ana Koleksiyonlar ---
    const val USERS              = "users"
    const val USERNAMES          = "usernames"
    const val POSTS              = "posts"
    const val COMMENTS           = "comments"
    const val POST_REACTIONS     = "postReactions"
    const val POST_RATINGS       = "postRatings"
    const val COMMENT_REACTIONS  = "commentReactions"
    const val REPORTS            = "reports"
    const val REFERENCES         = "references"
    const val REFERENCE_RELATIONS  = "referenceRelations"
    const val REFERENCE_RESPONSES  = "referenceResponses"
    const val REFERENCE_APPEALS    = "referenceAppeals"
    const val CONTRACTS            = "contracts"
    const val CONTRACT_VERSIONS    = "contractVersions"
    const val CONTRACT_TEMPLATES   = "contractTemplates"
    const val NOTIFICATIONS        = "notifications"
    const val CATEGORIES           = "categories"
    const val BLOCKED_USERS        = "blockedUsers"
    const val MODERATION_QUEUE     = "moderationQueue"
    const val MODERATION_ACTIONS   = "moderationActions"
    const val BANNED_TERMS         = "bannedTerms"
    const val PURCHASES            = "purchases"
    const val AD_SETTINGS          = "adSettings"
    const val APP_SETTINGS         = "appSettings"
    const val AUDIT_LOGS           = "auditLogs"

    // --- Belge Yolu Oluşturucular ---
    fun userPath(uid: String)           = "$USERS/$uid"
    fun usernamePath(username: String)  = "$USERNAMES/$username"
    fun postPath(postId: String)        = "$POSTS/$postId"
    fun commentPath(commentId: String)  = "$COMMENTS/$commentId"
    fun referencePath(refId: String)    = "$REFERENCES/$refId"
    fun contractPath(contractId: String) = "$CONTRACTS/$contractId"
    fun notificationPath(uid: String, notifId: String) = "$USERS/$uid/$NOTIFICATIONS/$notifId"
    fun purchasePath(uid: String)       = "$PURCHASES/$uid"

    // --- Alt Koleksiyon Yolları ---
    fun userNotificationsPath(uid: String) = "$USERS/$uid/$NOTIFICATIONS"
    fun postCommentsPath(postId: String)   = "$POSTS/$postId/$COMMENTS"

    // --- AppSettings belge ID'leri ---
    const val DOC_APP_SETTINGS = "config"
    const val DOC_AD_SETTINGS  = "ads"
}

/**
 * Firebase Storage yolu sabitleri.
 * CLAUDE.md Bölüm 15: Görsel Yükleme
 * CLAUDE.md Bölüm 26: Firebase Storage güvenliği
 */
object FirebaseStoragePaths {

    // Profil fotoğrafları
    fun profilePhoto(uid: String): String = "profile_photos/$uid/avatar.jpg"

    // Gönderi görselleri
    fun postImage(uid: String, postId: String): String = "post_images/$uid/$postId.jpg"

    // Kira sözleşmesi PDF
    fun contractPdf(contractId: String): String = "contracts/$contractId/contract.pdf"
}
