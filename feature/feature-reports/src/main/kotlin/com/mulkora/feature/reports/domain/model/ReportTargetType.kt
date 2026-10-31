package com.mulkora.feature.reports.domain.model

/**
 * Şikayet hedef türü.
 * CLAUDE.md Bölüm 11: Şikâyet Sistemi.
 *
 * [value] Cloud Function'a gönderilen string değeridir.
 * core-common/ReportContentType ile birebir eşleşir.
 */
enum class ReportTargetType(val value: String) {
    POST("post"),
    COMMENT("comment"),
    USER("user"),
    REFERENCE("reference"),
    REFERENCE_RESPONSE("reference_response"),
}