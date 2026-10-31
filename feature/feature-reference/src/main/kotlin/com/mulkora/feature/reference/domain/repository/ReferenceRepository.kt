package com.mulkora.feature.reference.domain.repository

import com.mulkora.core.common.result.Result
import com.mulkora.feature.reference.domain.model.Reference
import com.mulkora.feature.reference.domain.model.ReferenceType
import com.mulkora.feature.reference.domain.model.RelationshipType
import kotlinx.coroutines.flow.Flow

/**
 * Referans Repository interface.
 * CLAUDE.md §13: Referans Sistemi
 * docs/16-reference-system-state-machine.md
 *
 * KURAL: Referans başlatma işlemi yalnızca Cloud Functions üzerinden yapılabilir.
 * İstemci doğrudan references koleksiyonuna yazamaz.
 */
interface ReferenceRepository {

    /**
     * Kullanıcıya verilen (alınan) referansları gerçek zamanlı izler.
     * Yalnızca onaylı (approved) referanslar gösterilir.
     */
    fun getReferencesForUser(uid: String): Flow<Result<List<Reference>>>

    /**
     * Kullanıcının verdiği referansları döndürür.
     */
    fun getReferencesGivenByUser(uid: String): Flow<Result<List<Reference>>>

    /**
     * Tek referansı gerçek zamanlı izler.
     */
    fun getReferenceById(referenceId: String): Flow<Result<Reference?>>

    /**
     * Bekleyen referans isteklerini döndürür.
     * (Mevcut kullanıcıya gelen, henüz yanıtlanmamış — status = "pending")
     */
    fun getPendingReferenceRequests(): Flow<Result<List<Reference>>>

    /**
     * Referans başlatma isteği gönderir.
     * CLAUDE.md §13: initiateReference Cloud Function'ını çağırır.
     * Cloud Function: toUid'ye onay bildirimi gönderir ve pending referans oluşturur.
     * Dönüş: oluşturulan referansın ID'si
     */
    suspend fun initiateReference(
        toUid: String,
        referenceType: ReferenceType,
        rating: Int,
        content: String,
        relationshipType: RelationshipType,
        propertyAddress: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Result<String>

    /**
     * Referans talebini kabul eder.
     * acceptReference Cloud Function'ını çağırır.
     * State: pending → accepted
     */
    suspend fun acceptReference(referenceId: String): Result<Unit>

    /**
     * Referans talebini reddeder.
     * rejectReference Cloud Function'ını çağırır.
     * State: pending → rejected
     */
    suspend fun rejectReference(referenceId: String): Result<Unit>

    /**
     * Referansa yanıt verir (kabul sonrası).
     * respondToReference Cloud Function'ını çağırır.
     */
    suspend fun respondToReference(
        referenceId: String,
        responseContent: String
    ): Result<Unit>

    /**
     * Referansı gönderir (içerik yazıldıktan sonra).
     * submitReference Cloud Function'ını çağırır.
     * CLAUDE.md §13: awaiting_confirmation → preparing → pending_approval
     * İçerik filtresi CF tarafında uygulanır; istemci doğrudan Firestore'a yazmaz.
     */
    suspend fun submitReference(
        referenceId: String,
        comment: String,
        relationshipType: RelationshipType,
    ): Result<Unit>

    /**
     * Referansa itiraz eder.
     * CLAUDE.md §13: İtiraz moderatör incelemesine gönderilir.
     * State: approved → disputed
     */
    suspend fun disputeReference(
        referenceId: String,
        reason: String
    ): Result<Unit>
}
