package com.mulkora.feature.reference.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.mulkora.core.common.model.ReferenceStatus
import com.mulkora.core.common.result.Result
import com.mulkora.feature.reference.domain.model.Reference
import com.mulkora.feature.reference.domain.model.ReferenceResponse
import com.mulkora.feature.reference.domain.model.ReferenceType
import com.mulkora.feature.reference.domain.model.RelationshipType
import com.mulkora.feature.reference.domain.repository.ReferenceRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ReferenceRepository implementasyonu.
 * CLAUDE.md §13: Referans Sistemi.
 *
 * Okuma işlemleri Firestore gerçek zamanlı listener'larıyla yapılır.
 * Yazma/aksiyon işlemleri (initiate, accept, reject, respond, dispute)
 * Cloud Functions üzerinden yapılır — istemci doğrudan koleksiyona yazamaz.
 */
@Singleton
class ReferenceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
) : ReferenceRepository {

    companion object {
        private const val REFERENCES = "references"
        // docs/16-reference-system-state-machine.md ile eşleşen durum değerleri
        private const val STATUS_APPROVED = "approved"
        private const val STATUS_INVITE_SENT = "invite_sent"
        private const val STATUS_AWAITING_CONFIRMATION = "awaiting_confirmation"
        private const val STATUS_PREPARING = "preparing"
        private const val STATUS_PENDING_APPROVAL = "pending_approval"
    }

    override fun getReferencesForUser(uid: String): Flow<Result<List<Reference>>> = callbackFlow {
        trySend(Result.Loading)
        val listener = firestore
            .collection(REFERENCES)
            .whereEqualTo("toUid", uid)
            .whereEqualTo("status", STATUS_APPROVED)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                val refs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toReferenceDto()?.toReference()
                } ?: emptyList()
                trySend(Result.Success(refs))
            }
        awaitClose { listener.remove() }
    }

    override fun getReferencesGivenByUser(uid: String): Flow<Result<List<Reference>>> = callbackFlow {
        trySend(Result.Loading)
        val listener = firestore
            .collection(REFERENCES)
            .whereEqualTo("fromUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                val refs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toReferenceDto()?.toReference()
                } ?: emptyList()
                trySend(Result.Success(refs))
            }
        awaitClose { listener.remove() }
    }

    override fun getReferenceById(referenceId: String): Flow<Result<Reference?>> = callbackFlow {
        trySend(Result.Loading)
        val listener = firestore
            .collection(REFERENCES)
            .document(referenceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                val ref = snapshot?.toReferenceDto()?.toReference()
                trySend(Result.Success(ref))
            }
        awaitClose { listener.remove() }
    }

    override fun getPendingReferenceRequests(): Flow<Result<List<Reference>>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Result.Error(IllegalStateException("Kullanıcı oturum açmamış.")))
            close()
            return@callbackFlow
        }
        trySend(Result.Loading)
        // Bekleyen davetler: invite_sent veya awaiting_confirmation — docs/16-reference-system-state-machine.md
        val listener = firestore
            .collection(REFERENCES)
            .whereEqualTo("toUid", uid)
            .whereIn("status", listOf(STATUS_INVITE_SENT, STATUS_AWAITING_CONFIRMATION))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                val refs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toReferenceDto()?.toReference()
                } ?: emptyList()
                trySend(Result.Success(refs))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun initiateReference(
        toUid: String,
        referenceType: ReferenceType,
        rating: Int,
        content: String,
        relationshipType: RelationshipType,
        propertyAddress: String?,
        startDate: String?,
        endDate: String?
    ): Result<String> = try {
        val payload = mutableMapOf<String, Any>(
            "toUid" to toUid,
            "referenceType" to referenceType.value,
            "rating" to rating,
            "content" to content,
            "relationshipType" to relationshipType.value,
        )
        propertyAddress?.let { payload["propertyAddress"] = it }
        startDate?.let { payload["startDate"] = it }
        endDate?.let { payload["endDate"] = it }

        val callResult = functions
            .getHttpsCallable("initiateReference")
            .call(payload)
            .await()
        val data = callResult.data as? Map<*, *>
        val referenceId = data?.get("referenceId") as? String
            ?: return Result.Error(Exception("Referans ID alınamadı."))
        Result.Success(referenceId)
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun acceptReference(referenceId: String): Result<Unit> = try {
        functions
            .getHttpsCallable("acceptReference")
            .call(mapOf("referenceId" to referenceId))
            .await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun rejectReference(referenceId: String): Result<Unit> = try {
        functions
            .getHttpsCallable("rejectReference")
            .call(mapOf("referenceId" to referenceId))
            .await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun respondToReference(
        referenceId: String,
        responseContent: String
    ): Result<Unit> = try {
        functions
            .getHttpsCallable("respondToReference")
            .call(mapOf(
                "referenceId" to referenceId,
                "responseContent" to responseContent,
            ))
            .await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun submitReference(
        referenceId: String,
        comment: String,
        relationshipType: RelationshipType,
    ): Result<Unit> = try {
        functions
            .getHttpsCallable("submitReference")
            .call(mapOf(
                "referenceId" to referenceId,
                "comment" to comment,
                "relationshipType" to relationshipType.value,
            ))
            .await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun disputeReference(
        referenceId: String,
        reason: String
    ): Result<Unit> = try {
        functions
            .getHttpsCallable("disputeReference")
            .call(mapOf(
                "referenceId" to referenceId,
                "reason" to reason,
            ))
            .await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }
}

// --- Firestore belge dönüşüm yardımcıları ---

private fun com.google.firebase.firestore.DocumentSnapshot.toReferenceDto(): ReferenceDto? {
    return try {
        ReferenceDto(
            id = id,
            fromUid = getString("fromUid") ?: return null,
            toUid = getString("toUid") ?: return null,
            fromDisplayName = getString("fromDisplayName") ?: "",
            fromUsername = getString("fromUsername") ?: "",
            fromPhotoUrl = getString("fromPhotoUrl"),
            toDisplayName = getString("toDisplayName") ?: "",
            toUsername = getString("toUsername") ?: "",
            toPhotoUrl = getString("toPhotoUrl"),
            referenceType = getString("referenceType") ?: "tenant_to_landlord",
            rating = (getLong("rating") ?: 3L).toInt(),
            content = getString("content") ?: "",
            status = getString("status") ?: "invite_sent",
            relationshipType = getString("relationshipType") ?: "former_tenant",
            propertyAddress = getString("propertyAddress"),
            startDate = getString("startDate"),
            endDate = getString("endDate"),
            response = get("response") as? Map<*, *>,
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt"),
        )
    } catch (e: Exception) {
        null
    }
}

private data class ReferenceDto(
    val id: String,
    val fromUid: String,
    val toUid: String,
    val fromDisplayName: String,
    val fromUsername: String,
    val fromPhotoUrl: String?,
    val toDisplayName: String,
    val toUsername: String,
    val toPhotoUrl: String?,
    val referenceType: String,
    val rating: Int,
    val content: String,
    val status: String,
    val relationshipType: String,
    val propertyAddress: String?,
    val startDate: String?,
    val endDate: String?,
    val response: Map<*, *>?,
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?,
) {
    fun toReference(): Reference = Reference(
        id = id,
        fromUid = fromUid,
        toUid = toUid,
        fromDisplayName = fromDisplayName,
        fromUsername = fromUsername,
        fromPhotoUrl = fromPhotoUrl,
        toDisplayName = toDisplayName,
        toUsername = toUsername,
        toPhotoUrl = toPhotoUrl,
        referenceType = ReferenceType.fromValue(referenceType),
        rating = rating,
        content = content,
        status = ReferenceStatus.fromValue(status),
        relationshipType = RelationshipType.fromValue(relationshipType),
        propertyAddress = propertyAddress,
        startDate = startDate,
        endDate = endDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        response = response?.let {
            ReferenceResponse(
                id = it["id"] as? String ?: "",
                respondentUid = it["respondentUid"] as? String ?: "",
                content = it["content"] as? String ?: "",
                status = it["status"] as? String ?: "visible",
                createdAt = it["createdAt"] as? Timestamp,
            )
        },
    )
}