package com.mulkora.feature.contracts.data.dto

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.mulkora.feature.contracts.domain.model.Contract
import com.mulkora.feature.contracts.domain.model.ContractStatus
import com.mulkora.feature.contracts.domain.model.PdfStatus
import java.util.Date

/**
 * Firestore 'contracts' koleksiyonuna karşılık gelen veri transfer nesnesi.
 *
 * Alan adları Firestore doküman şemasıyla birebir örtüşmelidir.
 * Dönüşüm için [toDomain] kullanılır; Firestore'a yazma işlemi
 * her zaman Cloud Functions üzerinden gerçekleşir, bu nedenle
 * ayrı bir toMap() fonksiyonu gerekmez.
 */
data class ContractDto(
    @DocumentId val id: String = "",
    val documentNumber: String = "",
    val version: Int = 1,
    val status: String = ContractStatus.DRAFT.name,

    // Taraflar
    val partyUids: List<String> = emptyList(),
    val party1Uid: String = "",
    val party1DisplayName: String = "",
    val party1Approved: Boolean = false,
    val party2Uid: String? = null,
    val party2DisplayName: String? = null,
    val party2Approved: Boolean = false,

    // Taşınmaz
    val propertyAddress: String = "",
    val propertyType: String = "",

    // Kira koşulları
    val monthlyRent: Double = 0.0,
    val paymentDay: Int = 1,
    val paymentMethod: String = "",
    val depositAmount: Double = 0.0,
    val monthlyFee: Double? = null,

    // Dönem
    val startDate: String = "",
    val endDate: String = "",

    // İçerik
    val specialConditions: String? = null,
    val inventoryItems: List<String> = emptyList(),

    // Güvenlik
    val isLocked: Boolean = false,
    val verificationCode: String = "",
    val integrityHash: String? = null,

    // PDF
    val pdfStatus: String = PdfStatus.NOT_GENERATED.name,
    val pdfUrl: String? = null,

    // Zaman
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null,
) {
    fun toDomain(): Contract = Contract(
        id = id,
        documentNumber = documentNumber,
        version = version,
        status = runCatching { ContractStatus.valueOf(status) }.getOrDefault(ContractStatus.DRAFT),
        party1Uid = party1Uid,
        party1DisplayName = party1DisplayName,
        party1Approved = party1Approved,
        party2Uid = party2Uid,
        party2DisplayName = party2DisplayName,
        party2Approved = party2Approved,
        propertyAddress = propertyAddress,
        propertyType = propertyType,
        monthlyRent = monthlyRent,
        paymentDay = paymentDay,
        paymentMethod = paymentMethod,
        depositAmount = depositAmount,
        monthlyFee = monthlyFee,
        startDate = startDate,
        endDate = endDate,
        specialConditions = specialConditions,
        inventoryItems = inventoryItems,
        isLocked = isLocked,
        verificationCode = verificationCode,
        integrityHash = integrityHash,
        pdfStatus = runCatching { PdfStatus.valueOf(pdfStatus) }.getOrDefault(PdfStatus.NOT_GENERATED),
        pdfUrl = pdfUrl,
        createdAt = createdAt?.time ?: 0L,
        updatedAt = updatedAt?.time ?: 0L,
    )
}