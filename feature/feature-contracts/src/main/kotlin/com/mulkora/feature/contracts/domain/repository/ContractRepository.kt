package com.mulkora.feature.contracts.domain.repository

import com.mulkora.core.common.result.Result
import com.mulkora.feature.contracts.domain.model.Contract
import kotlinx.coroutines.flow.Flow

/**
 * Sözleşme modülü için repository arayüzü.
 *
 * CLAUDE.md §14: Sözleşme akışı — taslak oluşturma, taraf daveti,
 * onaylama, kilitleme ve PDF üretimi adımlarını kapsar.
 * CLAUDE.md §26: Sözleşmeler yalnızca taraflar ve yetkili yöneticiler
 * tarafından okunabilir; güvenlik kuralları backend ve Firestore Rules
 * katmanında uygulanır.
 */
interface ContractRepository {

    /** Kullanıcının dahil olduğu tüm sözleşmeleri gerçek zamanlı dinler. */
    fun observeUserContracts(): Flow<Result<List<Contract>>>

    /** Tek bir sözleşmeyi id ile gerçek zamanlı dinler. */
    fun observeContract(contractId: String): Flow<Result<Contract>>

    /**
     * Mevcut kullanıcıyı party1 olarak belirleyerek yeni bir sözleşme taslağı oluşturur.
     * Başarı durumunda oluşturulan sözleşmenin id'sini döner.
     */
    suspend fun createContract(input: CreateContractInput): Result<String>

    /**
     * İkinci tarafı e-posta veya kullanıcı adı ile sözleşmeye davet eder.
     * Cloud Function: inviteContractParty
     */
    suspend fun inviteParty(contractId: String, inviteIdentifier: String): Result<Unit>

    /**
     * Mevcut kullanıcının onayını kaydeder.
     * Her iki taraf da onayladıysa true döner (kilitleme tetiklenir).
     * Cloud Function: approveContract
     */
    suspend fun approveContract(contractId: String): Result<Boolean>

    /**
     * Sözleşmeyi değiştirilemez hale getirir ve hash oluşturur.
     * Cloud Function: lockContractVersion
     */
    suspend fun lockContract(contractId: String): Result<Unit>

    /**
     * Düzenleme talebi oluşturur; durum EDIT_REQUESTED'a çekilir.
     * Cloud Function: requestContractEdit
     */
    suspend fun requestEdit(contractId: String, note: String): Result<Unit>

    /**
     * PDF oluşturma işlemini başlatır veya hazır URL'yi döner.
     * Cloud Function: generateContractPdf
     */
    suspend fun generatePdf(contractId: String): Result<String>

    /** Sözleşme taslağını iptal eder. Yalnızca DRAFT veya AWAITING_PARTY2 durumunda çalışır. */
    suspend fun cancelContract(contractId: String): Result<Unit>
}

data class CreateContractInput(
    val propertyAddress: String,
    val propertyType: String,
    val monthlyRent: Double,
    val paymentDay: Int,
    val paymentMethod: String,
    val depositAmount: Double,
    val monthlyFee: Double?,
    val startDate: String,
    val endDate: String,
    val specialConditions: String?,
    val inventoryItems: List<String>,
)