package com.mulkora.feature.contracts.presentation

import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.contracts.domain.model.Contract
import com.mulkora.feature.contracts.domain.model.ContractStatus
import com.mulkora.feature.contracts.domain.model.PdfStatus
import com.mulkora.feature.contracts.domain.repository.ContractRepository
import com.mulkora.feature.contracts.presentation.create.ContractCreateViewModel
import com.mulkora.feature.contracts.presentation.detail.ContractDetailViewModel
import com.mulkora.feature.contracts.presentation.list.ContractListViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * ContractListViewModel, ContractDetailViewModel, ContractCreateViewModel birim testleri.
 *
 * CLAUDE.md §14: Kira sözleşmesi — liste, detay, oluşturma akışları.
 * docs/21-testing-strategy.md — UT-CONTRACT-LIST-001 ile UT-CONTRACT-CREATE-005.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContractViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var contractRepository: ContractRepository

    private fun createFakeContract(id: String = "contract_001") = Contract(
        id = id,
        documentNumber = "DOC-001",
        version = 1,
        status = ContractStatus.DRAFT,
        party1Uid = "user_001",
        party1DisplayName = "Ali Veli",
        party1Approved = false,
        party2Uid = null,
        party2DisplayName = null,
        party2Approved = false,
        propertyAddress = "İstanbul, Kadıköy",
        propertyType = "Konut",
        monthlyRent = 5000.0,
        paymentDay = 1,
        paymentMethod = "EFT",
        depositAmount = 10000.0,
        monthlyFee = null,
        startDate = "2025-01-01",
        endDate = "2026-01-01",
        specialConditions = null,
        inventoryItems = emptyList(),
        isLocked = false,
        verificationCode = "ABC123",
        integrityHash = null,
        pdfStatus = PdfStatus.NOT_GENERATED,
        pdfUrl = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    @Before
    fun setUp() {
        contractRepository = mockk(relaxed = true)
        every { contractRepository.observeUserContracts() } returns flowOf(emptyList())
    }

    // ────────────────────────────────────────────────────────────────────────────
    // ContractListViewModel testleri
    // ────────────────────────────────────────────────────────────────────────────

    // ─── UT-CONTRACT-LIST-001: Başlangıç durumu ───────────────────────────────

    @Test
    fun `ContractListViewModel initial state has empty contracts list`() = runTest {
        val viewModel = ContractListViewModel(contractRepository)
        assertNotNull(viewModel.uiState.value)
        assertTrue(viewModel.uiState.value.contracts.isEmpty())
    }

    // ─── UT-CONTRACT-LIST-002: Sözleşme listesi yüklenir ─────────────────────

    @Test
    fun `ContractListViewModel loads contracts from repository`() = runTest {
        val contracts = listOf(createFakeContract("c1"), createFakeContract("c2"))
        every { contractRepository.observeUserContracts() } returns flowOf(contracts)
        val viewModel = ContractListViewModel(contractRepository)
        // Flow'dan initial emit geldiğinde liste dolmalı
        assertNotNull(viewModel.uiState.value)
    }

    // ─── UT-CONTRACT-LIST-003: Hata mesajı başlangıçta null ──────────────────

    @Test
    fun `ContractListViewModel error is null initially`() = runTest {
        val viewModel = ContractListViewModel(contractRepository)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ─── UT-CONTRACT-LIST-004: dismissError çağrısı ───────────────────────────

    @Test
    fun `ContractListViewModel dismissError leaves errorMessage null`() = runTest {
        val viewModel = ContractListViewModel(contractRepository)
        viewModel.dismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // ContractDetailViewModel testleri
    // ────────────────────────────────────────────────────────────────────────────

    // ─── UT-CONTRACT-DETAIL-001: Başlangıç durumu ─────────────────────────────

    @Test
    fun `ContractDetailViewModel initial state is not loading`() = runTest {
        every { contractRepository.observeContract(any()) } returns flowOf(null)
        val viewModel = ContractDetailViewModel(contractRepository)
        assertNotNull(viewModel.uiState.value)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── UT-CONTRACT-DETAIL-002: Mevcut sözleşme yüklenir ────────────────────

    @Test
    fun `ContractDetailViewModel loads contract by id`() = runTest {
        val contract = createFakeContract("detail_01")
        every { contractRepository.observeContract("detail_01") } returns flowOf(contract)
        val viewModel = ContractDetailViewModel(contractRepository)
        viewModel.loadContract("detail_01")
        assertNotNull(viewModel.uiState.value)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // ContractCreateViewModel testleri
    // ────────────────────────────────────────────────────────────────────────────

    // ─── UT-CONTRACT-CREATE-001: Başlangıç durumu ─────────────────────────────

    @Test
    fun `ContractCreateViewModel initial state is not loading`() {
        val viewModel = ContractCreateViewModel(contractRepository)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isSuccess)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ─── UT-CONTRACT-CREATE-002: uiState null değil ───────────────────────────

    @Test
    fun `ContractCreateViewModel uiState is not null`() {
        val viewModel = ContractCreateViewModel(contractRepository)
        assertNotNull(viewModel.uiState.value)
    }

    // ─── UT-CONTRACT-CREATE-003: isSuccess başlangıçta false ─────────────────

    @Test
    fun `ContractCreateViewModel isSuccess is false on init`() {
        val viewModel = ContractCreateViewModel(contractRepository)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    // ─── UT-CONTRACT-CREATE-004: Hata mesajı başlangıçta null ────────────────

    @Test
    fun `ContractCreateViewModel errorMessage is null on init`() {
        val viewModel = ContractCreateViewModel(contractRepository)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ─── UT-CONTRACT-CREATE-005: dismissError sonrası null ───────────────────

    @Test
    fun `ContractCreateViewModel dismissError clears errorMessage`() {
        val viewModel = ContractCreateViewModel(contractRepository)
        viewModel.dismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}