package com.mulkora.feature.billing.presentation

import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.billing.domain.model.PurchaseStatus
import com.mulkora.feature.billing.domain.repository.BillingRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.mulkora.core.common.result.Result

/**
 * BillingViewModel birim testleri.
 *
 * CLAUDE.md §18: Satın alma hakları istemciden değiştirilemez;
 * ViewModel yalnızca UI durumunu yönetir, BillingRepository mock'lanır.
 * docs/21-testing-strategy.md — UT-BILLING-001 ile UT-BILLING-005.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BillingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var billingRepository: BillingRepository
    private lateinit var viewModel: BillingViewModel

    @Before
    fun setUp() {
        billingRepository = mockk(relaxed = true)
        every { billingRepository.observePurchaseStatus() } returns flowOf(PurchaseStatus.NOT_PURCHASED)
        viewModel = BillingViewModel(billingRepository)
    }

    // ─── UT-BILLING-001: Başlangıç durumu ─────────────────────────────────────

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.isAdFree)
    }

    // ─── UT-BILLING-002: uiState null değil ───────────────────────────────────

    @Test
    fun `uiState is not null after init`() {
        assertNotNull(viewModel.uiState.value)
    }

    // ─── UT-BILLING-003: isLoading başlangıçta false ──────────────────────────

    @Test
    fun `isLoading is false on init`() {
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── UT-BILLING-004: Hata mesajı başlangıçta null ────────────────────────

    @Test
    fun `errorMessage is null on init`() {
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ─── UT-BILLING-005: clearError sonrası hata null ────────────────────────

    @Test
    fun `clearError leaves errorMessage null`() = runTest {
        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ─── UT-BILLING-006: Satın alma başarılı — isLoading false ──────────────

    @Test
    fun `purchaseRemoveAds success leaves isLoading false`() = runTest {
        coEvery { billingRepository.launchRemoveAdsPurchase() } returns Result.Success(Unit)
        viewModel.purchaseRemoveAds()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── UT-BILLING-007: Satın alma hatası — errorMessage set edilir ─────────

    @Test
    fun `purchaseRemoveAds error sets errorMessage`() = runTest {
        coEvery { billingRepository.launchRemoveAdsPurchase() } returns
            Result.Error(Exception("Satın alma başlatılamadı"))
        viewModel.purchaseRemoveAds()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    // ─── UT-BILLING-008: PURCHASED durumunda isAdFree true ───────────────────

    @Test
    fun `isAdFree is true when status is PURCHASED`() = runTest {
        val statusFlow = MutableStateFlow(PurchaseStatus.NOT_PURCHASED)
        every { billingRepository.observePurchaseStatus() } returns statusFlow
        val vm = BillingViewModel(billingRepository)

        statusFlow.value = PurchaseStatus.PURCHASED
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isAdFree)
    }

    // ─── UT-BILLING-009: restorePurchases başarılı ───────────────────────────

    @Test
    fun `restorePurchases success leaves isLoading false`() = runTest {
        coEvery { billingRepository.restorePurchases() } returns Result.Success(Unit)
        viewModel.restorePurchases()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── UT-BILLING-010: disconnectBilling onCleared'da çağrılır ─────────────

    @Test
    fun `disconnectBilling is called on cleared`() {
        viewModel.onCleared()
        verify { billingRepository.disconnectBilling() }
    }
}