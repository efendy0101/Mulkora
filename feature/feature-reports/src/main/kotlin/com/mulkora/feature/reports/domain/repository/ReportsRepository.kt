package com.mulkora.feature.reports.domain.repository

import com.mulkora.core.common.model.ReportReason
import com.mulkora.core.common.result.Result
import com.mulkora.feature.reports.domain.model.ReportTargetType

/**
 * Şikâyet modülü repository arayüzü.
 *
 * CLAUDE.md §11: Şikâyet iş kuralları:
 * - Şikâyet oluşturma yalnızca createReport Cloud Function üzerinden yapılır.
 * - Aynı kullanıcı aynı içeriği yalnızca bir kez şikâyet edebilir.
 */
interface ReportsRepository {

    /**
     * Şikâyet oluşturur.
     * CLAUDE.md §11: createReport Cloud Function çağrılır.
     */
    suspend fun createReport(
        targetId: String,
        targetType: ReportTargetType,
        reason: ReportReason,
        description: String?,
    ): Result<Unit>
}