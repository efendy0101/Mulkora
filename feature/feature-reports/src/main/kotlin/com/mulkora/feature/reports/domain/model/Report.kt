package com.mulkora.feature.reports.domain.model

import com.mulkora.core.common.model.ReportReason

/**
 * Şikâyet durumu.
 *
 * CLAUDE.md §11: Şikâyet durumları.
 */
enum class ReportStatus {
    NEW,
    UNDER_REVIEW,
    AWAITING_MORE_INFO,
    ACTION_TAKEN,
    DISMISSED,
    CLOSED,
}

/**
 * Şikâyet domain modeli.
 *
 * CLAUDE.md §11:
 * - Aynı kullanıcı aynı içeriği yalnızca bir kez şikâyet edebilir.
 * - Şikâyet oluşturma yalnızca Cloud Functions üzerinden gerçekleşir.
 */
data class Report(
    val id: String,
    val reporterId: String,
    val targetId: String,
    val targetType: ReportTargetType,
    val reason: ReportReason,
    val description: String?,
    val status: ReportStatus,
    val moderatorNote: String?,
    val createdAt: Long,
    val updatedAt: Long,
)