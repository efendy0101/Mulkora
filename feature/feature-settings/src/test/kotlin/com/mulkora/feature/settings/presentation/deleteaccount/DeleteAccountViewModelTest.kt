package com.mulkora.feature.settings.presentation.deleteaccount

import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.auth.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * DeleteAccountViewModel birim testleri.
 *
 * CLAUDE.md §26: Hesap silme — kullanıcı yazılı onay metnini girerek hesabını siler.
 * docs/21-testing-strategy.md — UT-SETTINGS-DELETE-001 ile UT-SETTINGS-DELETE-006.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: DeleteAccountViewModel

    @Before
    fun setUp() {
        authRepository = mockk(relaxed = true)
        viewModel = DeleteAccountViewModel(authRepository)
    }

    // ─── UT-SETTINGS-DELETE-001: Başlangıç durumu ────────────────────────────

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertTrue(state.confirmationText.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isDeleted)
        assertNull(state.error)
    }

    // ─── UT-SETTINGS-DELETE-002: Onay metni güncellenir ─────────────────────

    @Test
    fun `onConfirmationTextChange updates text`() {
        viewModel.onConfirmationTextChange("HESABIMI SİL")
        assertTrue(viewModel.uiState.value.confirmationText.isNotEmpty())
    }

    // ─── UT-SETTINGS-DELETE-003: Yanlış metin silme işlemini engeller ────────

    @Test
    fun `onDeleteAccountClicked with wrong text sets error`() = runTest {
        viewModel.onConfirmationTextChange("yanlış metin")
        viewModel.onDeleteAccountClicked(onSuccess = {})
        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isDeleted)
    }

    // ─── UT-SETTINGS-DELETE-004: Boş metin silme işlemini engeller ───────────

    @Test
    fun `onDeleteAccountClicked with empty text sets error`() = runTest {
        viewModel.onDeleteAccountClicked(onSuccess = {})
        assertNotNull(viewModel.uiState.value.error)
    }

    // ─── UT-SETTINGS-DELETE-005: Doğru metin ile silme başarılı ─────────────

    @Test
    fun `onDeleteAccountClicked with correct text calls success`() = runTest {
        coEvery { authRepository.deleteAccount() } returns Unit
        viewModel.onConfirmationTextChange("HESABIMI SİL")
        var successCalled = false
        viewModel.onDeleteAccountClicked(onSuccess = { successCalled = true })
        assertTrue(successCalled || viewModel.uiState.value.isDeleted)
    }

    // ─── UT-SETTINGS-DELETE-006: Hata temizleme ──────────────────────────────

    @Test
    fun `clearError sets error to null`() = runTest {
        viewModel.onConfirmationTextChange("yanlış")
        viewModel.onDeleteAccountClicked(onSuccess = {})
        assertNotNull(viewModel.uiState.value.error)
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }
}