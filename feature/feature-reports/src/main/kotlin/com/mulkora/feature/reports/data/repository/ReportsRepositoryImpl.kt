package com.mulkora.feature.reports.data.repository

import com.google.firebase.functions.FirebaseFunctions
import com.mulkora.core.common.model.ReportReason
import com.mulkora.core.common.result.Result
import com.mulkora.feature.reports.domain.model.ReportTargetType
import com.mulkora.feature.reports.domain.repository.ReportsRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * ReportsRepository implementasyonu.
 *
 * CLAUDE.md §11: createReport işlemi Cloud Function üzerinden gerçekleştirilir.
 * İstemci Firestore'a doğrudan şikâyet belgesi yazamaz.
 */
class ReportsRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions,
) : ReportsRepository {

    override suspend fun createReport(
        targetId: String,
        targetType: ReportTargetType,
        reason: ReportReason,
        description: String?,
    ): Result<Unit> {
        return try {
            val params = mutableMapOf<String, Any>(
                "targetId" to targetId,
                "targetType" to targetType.name,
                "reason" to reason.name,
            )
            description?.let { params["description"] = it }
            functions.getHttpsCallable("createReport").call(params).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}