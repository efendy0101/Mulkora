package com.mulkora.feature.contracts.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.mulkora.core.common.result.Result
import com.mulkora.feature.contracts.data.dto.ContractDto
import com.mulkora.feature.contracts.domain.model.Contract
import com.mulkora.feature.contracts.domain.repository.ContractRepository
import com.mulkora.feature.contracts.domain.repository.CreateContractInput
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val COLLECTION_CONTRACTS = "contracts"

/**
 * ContractRepository'nin Firestore + Cloud Functions tabanlı gerçek uygulaması.
 *
 * CLAUDE.md §25: Tüm yazma işlemleri Cloud Functions üzerinden gerçekleştirilir.
 * CLAUDE.md §26: Sözleşmeler yalnızca taraflar ve yetkili yöneticiler tarafından
 * okunabilir (Firestore Security Rules ile de korunur).
 * CLAUDE.md §33: Compose ekranları doğrudan Firestore'a erişemez.
 */
@Singleton
class ContractRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth,
) : ContractRepository {

    private val contractsCol get() = firestore.collection(COLLECTION_CONTRACTS)

    override fun observeUserContracts(): Flow<Result<List<Contract>>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Result.Error(IllegalStateException("Kullanıcı oturumu bulunamadı")))
            close()
            return@callbackFlow
        }

        // createContract CF → partyUids: [creatorUid]
        // inviteContractParty CF → partyUids: arrayUnion(inviteeUid)
        // Tek sorgu ile hem party1 hem party2 sözleşmelerini çek.
        // firestore.indexes.json: partyUids ARRAY_CONTAINS + updatedAt DESC
        val listener = contractsCol
            .whereArrayContains("partyUids", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(Result.Error(err))
                    return@addSnapshotListener
                }
                val items = snap?.documents?.mapNotNull { doc ->
                    runCatching { doc.toObject(ContractDto::class.java)?.toDomain() }.getOrNull()
                } ?: emptyList()
                trySend(Result.Success(items))
            }

        awaitClose { listener.remove() }
    }

    override fun observeContract(contractId: String): Flow<Result<Contract>> = callbackFlow {
        val listener = contractsCol.document(contractId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(Result.Error(err))
                    return@addSnapshotListener
                }
                val contract = snap?.toObject(ContractDto::class.java)?.toDomain()
                if (contract != null) {
                    trySend(Result.Success(contract))
                } else {
                    trySend(Result.Error(NoSuchElementException("Sözleşme bulunamadı: $contractId")))
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createContract(input: CreateContractInput): Result<String> =
        runCatching {
            val data = mapOf(
                "propertyAddress" to input.propertyAddress,
                "propertyType" to input.propertyType,
                "monthlyRent" to input.monthlyRent,
                "paymentDay" to input.paymentDay,
                "paymentMethod" to input.paymentMethod,
                "depositAmount" to input.depositAmount,
                "monthlyFee" to input.monthlyFee,
                "startDate" to input.startDate,
                "endDate" to input.endDate,
                "specialConditions" to input.specialConditions,
                "inventoryItems" to input.inventoryItems,
            )
            val result = functions
                .getHttpsCallable("createContract")
                .call(data)
                .await()
            @Suppress("UNCHECKED_CAST")
            val resultMap = result.data as? Map<String, Any>
            resultMap?.get("contractId") as? String
                ?: throw IllegalStateException("contractId bulunamadı")
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it) },
        )

    override suspend fun inviteParty(contractId: String, inviteIdentifier: String): Result<Unit> =
        runCatching {
            // CF inviteContractParty → { contractId, inviteeUid } bekler
            // inviteIdentifier burada kullanıcı uid'si olarak iletilir
            functions.getHttpsCallable("inviteContractParty")
                .call(mapOf("contractId" to contractId, "inviteeUid" to inviteIdentifier))
                .await()
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(it) },
        )

    override suspend fun approveContract(contractId: String): Result<Boolean> =
        runCatching {
            val result = functions.getHttpsCallable("approveContract")
                .call(mapOf("contractId" to contractId))
                .await()
            @Suppress("UNCHECKED_CAST")
            val map = result.data as? Map<String, Any>
            map?.get("bothApproved") as? Boolean ?: false
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it) },
        )

    override suspend fun lockContract(contractId: String): Result<Unit> =
        runCatching {
            functions.getHttpsCallable("lockContractVersion")
                .call(mapOf("contractId" to contractId))
                .await()
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(it) },
        )

    override suspend fun requestEdit(contractId: String, note: String): Result<Unit> =
        runCatching {
            functions.getHttpsCallable("requestContractEdit")
                .call(mapOf("contractId" to contractId, "note" to note))
                .await()
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(it) },
        )

    override suspend fun generatePdf(contractId: String): Result<String> =
        runCatching {
            val result = functions.getHttpsCallable("generateContractPdf")
                .call(mapOf("contractId" to contractId))
                .await()
            @Suppress("UNCHECKED_CAST")
            val map = result.data as? Map<String, Any>
            map?.get("pdfUrl") as? String
                ?: throw IllegalStateException("pdfUrl bulunamadı")
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it) },
        )

    override suspend fun cancelContract(contractId: String): Result<Unit> =
        runCatching {
            functions.getHttpsCallable("cancelContract")
                .call(mapOf("contractId" to contractId))
                .await()
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(it) },
        )
}