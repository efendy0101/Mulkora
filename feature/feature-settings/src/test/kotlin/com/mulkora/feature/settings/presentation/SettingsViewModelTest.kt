package com.mulkora.feature.settings.presentation

import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.auth.domain.repository.AuthRepository
import com.mulkora.feature.billing.domain.repository.BillingRepository
import com.mulkora.feature.profile.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * SettingsViewModel birim testleri.
 *
 * Test ID aralığı: UT-SETTINGS-001 → UT-SETTINGS-010
 * docs/21-testing-strategy.md — ViewModel unit test stratejisi
 * CLAUDE.md §33: Üretim kodunda TODO bırakma; testleri ekle.
 */
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk()
    private val profileRepository: ProfileRepository = mockk()
    private val billingRepository: BillingRepository = mockk()

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        viewModel = SettingsViewModel(authRepository, profileRepository, billingRepository)
    }

    // UT-SETTINGS-001 — Başlangıç durumu
    @Test
    fun `initial state is not loading, no dialog, no snackbar`() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.showDeleteAccountDialog)
        assertNull(state.snackbarMessage)
        assertNull(state.navigationEvent)
    }

    // UT-SETTINGS-002 — Çıkış başarılı → NavigateToLogin
    @Test
    fun `onSignOutClick success navigates to login`() = runTest {
        coEvery { authRepository.signOut() } returns Result.Success(Unit)

        viewModel.onSignOutClick()

        assertEquals(
            SettingsNavigationEvent.NavigateToLogin,
            viewModel.uiState.value.navigationEvent
        )
    }

    // UT-SETTINGS-003 — Çıkış başarısız → snackbar mesajı
    @Test
    fun `onSignOutClick failure shows snackbar`() = runTest {
        coEvery { authRepository.signOut() } returns Result.Error(Exception("network error"))

        viewModel.onSignOutClick()

        assertTrue(viewModel.uiState.value.snackbarMessage != null)
        assertNull(viewModel.uiState.value.navigationEvent)
    }

    // UT-SETTINGS-004 — Hesap silme dialogunu aç
    @Test
    fun `onDeleteAccountClick shows dialog`() {
        viewModel.onDeleteAccountClick()

        assertTrue(viewModel.uiState.value.showDeleteAccountDialog)
    }

    // UT-SETTINGS-005 — Hesap silme dialogunu kapat
    @Test
    fun `onDeleteAccountDismissed hides dialog`() {
        viewModel.onDeleteAccountClick()
        viewModel.onDeleteAccountDismissed()

        assertFalse(viewModel.uiState.value.showDeleteAccountDialog)
    }

    // UT-SETTINGS-006 — Hesap silme onaylandı → başarılı → NavigateToLogin
    @Test
    fun `onDeleteAccountConfirmed success navigates to login`() = runTest {
        coEvery { authRepository.deleteAccount() } returns Result.Success(Unit)

        viewModel.onDeleteAccountConfirmed()

        assertEquals(
            SettingsNavigationEvent.NavigateToLogin,
            viewModel.uiState.value.navigationEvent
        )
    }

    // UT-SETTINGS-007 — Hesap silme onaylandı → başarısız → snackbar
    @Test
    fun `onDeleteAccountConfirmed failure shows snackbar`() = runTest {
        coEvery { authRepository.deleteAccount() } returns Result.Error(Exception("delete failed"))

        viewModel.onDeleteAccountConfirmed()

        assertTrue(viewModel.uiState.value.snackbarMessage != null)
        assertNull(viewModel.uiState.value.navigationEvent)
    }

    // UT-SETTINGS-008 — Veri dışa aktarma talebi başarılı → snackbar
    @Test
    fun `onRequestDataExportClick success shows success snackbar`() = runTest {
        coEvery { profileRepository.requestDataExport() } returns Result.Success(Unit)

        viewModel.onRequestDataExportClick()

        assertTrue(viewModel.uiState.value.snackbarMessage != null)
    }

    // UT-SETTINGS-009 — Satın alma geri yükleme başarılı → snackbar
    @Test
    fun `onRestorePurchasesClick success shows success snackbar`() = runTest {
        coEvery { billingRepository.restorePurchases() } returns Result.Success(true)

        viewModel.onRestorePurchasesClick()

        assertTrue(viewModel.uiState.value.snackbarMessage != null)
    }

    // UT-SETTINGS-010 — Satın alma geri yükleme başarısız → hata snackbar
    @Test
    fun `onRestorePurchasesClick failure shows error snackbar`() = runTest {
        coEvery { billingRepository.restorePurchases() } returns Result.Error(Exception("restore failed"))

        viewModel.onRestorePurchasesClick()

        assertTrue(viewModel.uiState.value.snackbarMessage != null)
    }

    // UT-SETTINGS-011 — Snackbar temizlenir
    @Test
    fun `onSnackbarDismissed clears snackbar message`() = runTest {
        coEvery { authRepository.signOut() } returns Result.Error(Exception("error"))
        viewModel.onSignOutClick()

        viewModel.onSnackbarDismissed()

        assertNull(viewModel.uiState.value.snackbarMessage)
    }

    // UT-SETTINGS-012 — Email doğrulama gönderme çağrıldı
    @Test
    fun `onSendEmailVerificationClick calls authRepository`() = runTest {
        coEvery { authRepository.sendEmailVerification() } returns Result.Success(Unit)

        viewModel.onSendEmailVerificationClick()

        coVerify { authRepository.sendEmailVerification() }
    }
}