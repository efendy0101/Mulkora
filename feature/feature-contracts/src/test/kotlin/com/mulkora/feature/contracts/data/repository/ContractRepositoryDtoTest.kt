package com.mulkora.feature.contracts.data.repository

import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.contracts.data.dto.ContractDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Date

/**
 * ContractDto ve ContractRepository veri modeli birim testleri.
 *
 * CLAUDE.md §14: Kira sözleşmesi — DTO dönüşüm mantığı.
 * docs/21-testing-strategy.md — UT-CONTRACT-001 ile UT-CONTRACT-006.
 *
 * Not: FirebaseFirestore doğrudan test edilemeyeceğinden DTO yapısı
 * ve alan dönüşümleri test edilir.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContractRepositoryDtoTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ─── UT-CONTRACT-001: ContractDto varsayılan değerleri ───────────────────

    @Test
    fun `ContractDto default values are correct`() {
        val dto = ContractDto()
        assertNotNull(dto)
        assertTrue(dto.partyUids.isEmpty())
        assertNull(dto.pdfUrl)
        assertEquals("draft", dto.status)
    }

    // ─── UT-CONTRACT-002: ContractDto alanları doğru atanır ─────────────────

    @Test
    fun `ContractDto fields are assigned correctly`() {
        val dto = ContractDto(
            id = "contract_001",
            createdByUid = "user_001",
            tenantUid = "tenant_001",
            landlordUid = "landlord_001",
            partyUids = listOf("user_001", "tenant_001"),
            status = "pending_signature",
            pdfUrl = "https://example.com/contract.pdf",
            createdAt = Date(),
            updatedAt = Date()
        )

        assertEquals("contract_001", dto.id)
        assertEquals("user_001", dto.createdByUid)
        assertEquals("pending_signature", dto.status)
        assertNotNull(dto.pdfUrl)
        assertEquals(2, dto.partyUids.size)
    }

    // ─── UT-CONTRACT-003: partyUids iki tarafı içermelidir ──────────────────

    @Test
    fun `partyUids contains both tenant and landlord`() {
        val tenantId = "tenant_uid"
        val landlordId = "landlord_uid"
        val dto = ContractDto(
            partyUids = listOf(tenantId, landlordId)
        )

        assertTrue(dto.partyUids.contains(tenantId))
        assertTrue(dto.partyUids.contains(landlordId))
    }

    // ─── UT-CONTRACT-004: Draft durumunda pdfUrl null olabilir ──────────────

    @Test
    fun `draft contract has null pdfUrl`() {
        val dto = ContractDto(status = "draft")
        assertNull(dto.pdfUrl)
        assertEquals("draft", dto.status)
    }

    // ─── UT-CONTRACT-005: Signed durumunda pdfUrl dolu olmalı ───────────────

    @Test
    fun `signed contract has pdfUrl`() {
        val dto = ContractDto(
            status = "signed",
            pdfUrl = "https://storage.firebase.com/contracts/contract_001.pdf"
        )

        assertEquals("signed", dto.status)
        assertNotNull(dto.pdfUrl)
        assertFalse(dto.pdfUrl!!.isEmpty())
    }

    // ─── UT-CONTRACT-006: ContractDto kopyalama tutarlılığı ─────────────────

    @Test
    fun `ContractDto copy preserves unchanged fields`() {
        val original = ContractDto(
            id = "original_id",
            status = "draft",
            pdfUrl = null
        )
        val updated = original.copy(status = "signed", pdfUrl = "https://example.com/pdf")

        assertEquals("original_id", updated.id)
        assertEquals("signed", updated.status)
        assertNotNull(updated.pdfUrl)
    }
}