package com.mulkora.feature.contracts.domain.model

/**
 * Uygulama katmanında kullanılan kira sözleşmesi domain modeli.
 *
 * Firestore'dan gelen veriler ContractDto → Contract dönüşümü ile
 * bu sınıfa çevrilir; Compose ekranları yalnızca bu sınıfı kullanır.
 *
 * CLAUDE.md §14: Kira sözleşmesi oluşturma akışı ve alanları.
 */
data class Contract(
    val id: String,
    val documentNumber: String,
    val version: Int,
    val status: ContractStatus,

    // Taraflar
    val party1Uid: String,
    val party1DisplayName: String,
    val party1Approved: Boolean,
    val party2Uid: String?,
    val party2DisplayName: String?,
    val party2Approved: Boolean,

    // Taşınmaz
    val propertyAddress: String,
    val propertyType: String,

    // Kira koşulları
    val monthlyRent: Double,
    val paymentDay: Int,
    val paymentMethod: String,
    val depositAmount: Double,
    val monthlyFee: Double?,

    // Dönem
    val startDate: String,
    val endDate: String,

    // İçerik
    val specialConditions: String?,
    val inventoryItems: List<String>,

    // Güvenlik
    val isLocked: Boolean,
    val verificationCode: String,
    val integrityHash: String?,

    // PDF
    val pdfStatus: PdfStatus,
    val pdfUrl: String?,

    // Zaman
    val createdAt: Long,
    val updatedAt: Long,
)

enum class ContractStatus {
    DRAFT,
    AWAITING_PARTY2,
    AWAITING_APPROVAL,
    EDIT_REQUESTED,
    BOTH_APPROVED,
    LOCKED,
    CANCELLED,
}

enum class PdfStatus {
    NOT_GENERATED,
    GENERATING,
    READY,
    FAILED,
}