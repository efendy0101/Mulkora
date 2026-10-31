package com.mulkora.feature.reference.presentation.response

import androidx.lifecycle.SavedStateHandle
import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.reference.domain.repository.ReferenceRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * ReferenceResponseViewModel birim testleri.
 *
 * CLAUDE.md §14: Referans Sistemi — yanıt süreci.
 * docs/16-reference-system-state-machine.md: PENDING → RESPONDED geçişi.
 * docs/21-testing-strategy.md — UT-REF-001 ile UT-REF-008.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReferenceResponseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var referenceRepository: ReferenceRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: ReferenceResponseViewModel

    private val testReferenceId = "ref_test_789"

    @Before
    fun setUp() {
        referenceRepository = mockk()
        savedStateHandle = SavedStateHandle(mapOf("referenceId" to testReferenceId))

        // Varsayılan: referans bulunamadı
        every {
            referenceRepository.getReferenceById(testReferenceId)
        } returns flowOf(Result.Failure(Exception("Referans bulunamadı")))

        viewModel = ReferenceResponseViewModel(savedStateHandle, referenceRepository)
    }

    // ─── UT-REF-001: Başlangıç durumu ────────────────────────────────────────

    @Test
    fun `initial state has empty responseContent`() {
        assertEquals("", viewModel.uiState.value.responseContent)
        assertFalse(viewModel.uiState.value.isSuccess)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ─── UT-REF-002: Yanıt içeriği değişimi ──────────────────────────────────

    @Test
    fun `onResponseContentChanged updates responseContent`() {
        viewModel.onResponseContentChanged("Bu bir referans yanıtıdır.")
        assertEquals("Bu bir referans yanıtıdır.", viewModel.uiState.value.responseContent)
    }

    // ─── UT-REF-003: MAX_RESPONSE_LENGTH sınırı ──────────────────────────────

    @Test
    fun `onResponseContentChanged truncates to MAX_RESPONSE_LENGTH`() {
        val longText = "a".repeat(1500) // MAX_RESPONSE_LENGTH = 1000
        viewModel.onResponseContentChanged(longText)
        assertEquals(
            ReferenceResponseViewModel.MAX_RESPONSE_LENGTH,
            viewModel.uiState.value.responseContent.length
        )
    }

    // ─── UT-REF-004: Boş yanıt gönderilemez ──────────────────────────────────

    @Test
    fun `onSubmit with empty response sets errorMessage`() = runTest {
        viewModel.onResponseContentChanged("")
        viewModel.onSubmit()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    // ─── UT-REF-005: MIN_RESPONSE_LENGTH altı yanıt gönderilemez ─────────────

    @Test
    fun `onSubmit with too short response sets errorMessage`() = runTest {
        viewModel.onResponseContentChanged("kısa") // < MIN_RESPONSE_LENGTH (10)
        viewModel.onSubmit()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    // ─── UT-REF-006: Başarılı yanıt gönderme ─────────────────────────────────

    @Test
    fun `onSubmit success sets isSuccess true`() = runTest {
        val validResponse = "Bu yeterince uzun bir referans yanıtıdır."

        coEvery {
            referenceRepository.respondToReference(testReferenceId, validResponse)
        } returns Result.Success(Unit)

        viewModel.onResponseContentChanged(validResponse)
        viewModel.onSubmit()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ─── UT-REF-007: Başarısız yanıt gönderme ────────────────────────────────

    @Test
    fun `onSubmit failure sets errorMessage`() = runTest {
        val validResponse = "Bu yeterince uzun bir referans yanıtıdır."

        coEvery {
            referenceRepository.respondToReference(testReferenceId, validResponse)
        } returns Result.Failure(Exception("Yanıt gönderilemedi"))

        viewModel.onResponseContentChanged(validResponse)
        viewModel.onSubmit()

        assertFalse(viewModel.uiState.value.isSuccess)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    // ─── UT-REF-008: Hata dismiss ────────────────────────────────────────────

    @Test
    fun `onErrorDismissed clears errorMessage`() = runTest {
        viewModel.onResponseContentChanged("")
        viewModel.onSubmit()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onErrorDismissed()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}