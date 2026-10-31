package com.mulkora.core.common.model

/**
 * Şikayet nedenleri — CLAUDE.md Bölüm 11: Şikâyet Sistemi.
 * Bu enum Cloud Function'da ve Firestore'da string olarak saklanır.
 */
enum class ReportReason(val displayName: String) {
    INSULT_OR_PROFANITY("Hakaret veya küfür"),
    HARASSMENT("Taciz"),
    THREAT("Tehdit"),
    HATE_SPEECH("Nefret söylemi"),
    SPAM("Spam"),
    MISINFORMATION("Yanlış bilgi"),
    PERSONAL_INFO_SHARING("Kişisel bilgi paylaşımı"),
    DEFAMATION("İftira veya hedef gösterme"),
    INAPPROPRIATE_IMAGE("Uygunsuz görsel"),
    ADVERTISEMENT("Reklam"),
    OFF_TOPIC("Konu dışı içerik"),
    FAKE_REFERENCE("Sahte referans"),
    OTHER("Diğer"),
}

/**
 * Şikayet edilebilecek içerik türleri — CLAUDE.md Bölüm 11.
 */
enum class ReportContentType(val value: String) {
    POST("post"),
    COMMENT("comment"),
    USER("user"),
    REFERENCE("reference"),
    REFERENCE_RESPONSE("reference_response"),
}

/**
 * Şikayet durumları — CLAUDE.md Bölüm 11.
 */
enum class ReportStatus(val displayName: String) {
    NEW("Yeni"),
    UNDER_REVIEW("İnceleniyor"),
    AWAITING_INFO("Ek bilgi bekleniyor"),
    ACTION_TAKEN("İşlem yapıldı"),
    REJECTED("Reddedildi"),
    CLOSED("Kapatıldı"),
}