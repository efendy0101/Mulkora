package com.mulkora.feature.auth.presentation.termsacceptance

import com.google.common.truth.Truth.assertThat
import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.auth.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * TermsAcceptanceViewModelTest — KVKK / Kullanım Koşulları kabul ekranı senaryoları.
 *
 * CLAUDE.md §5: Gizlilik politikası ve kullanım koşulları onaylanmadan devam edilemez.
 * docs/21-testing-strategy.md — UT-AUTH kapsamı.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TermsAcceptanceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: TermsAcceptanceViewModel

    @Before
    fun setup() {
        authRepository = mockk()
        viewModel = TermsAcceptanceViewModel(authRepository)
    }

    // ============================================================
    // Başlangıç Durumu
    // ============================================================

    @Test
    fun `başlangıçta koşullar kabul edilmemiş ve devam edilemiyor`() {
        val state = viewModel.uiState.value
        assertThat(state.isTermsAccepted).isFalse()
        assertThat(state.isPrivacyAccepted).isFalse()
        assertThat(state.canProceed).isFalse()
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    // ============================================================
    // Koşul Onayları
    // ============================================================

    @Test
    fun `kullanım koşulları onaylandığında isTermsAccepted true olur`() {
        viewModel.onTermsAcceptedChanged(true)
        assertThat(viewModel.uiState.value.isTermsAccepted).isTrue()
    }

    @Test
    fun `gizlilik politikası onaylandığında isPrivacyAccepted true olur`() {
        viewModel.onPrivacyAcceptedChanged(true)
        assertThat(viewModel.uiState.value.isPrivacyAccepted).isTrue()
    }

    @Test
    fun `her ikisi de onaylandığında canProceed true olur`() {
        viewModel.onTermsAcceptedChanged(true)
        viewModel.onPrivacyAcceptedChanged(true)
        assertThat(viewModel.uiState.value.canProceed).isTrue()
    }

    @Test
    fun `sadece biri onaylandığında canProceed false kalır`() {
        viewModel.onTermsAcceptedChanged(true)
        assertThat(viewModel.uiState.value.canProceed).isFalse()

        viewModel.onTermsAcceptedChanged(false)
        viewModel.onPrivacyAcceptedChanged(true)
        assertThat(viewModel.uiState.value.canProceed).isFalse()
    }

    @Test
    fun `onay geri alındığında canProceed false olur`() {
        viewModel.onTermsAcceptedChanged(true)
        viewModel.onPrivacyAcceptedChanged(true)
        assertThat(viewModel.uiState.value.canProceed).isTrue()

        viewModel.onTermsAcceptedChanged(false)
        assertThat(viewModel.uiState.value.canProceed).isFalse()
    }

    // ============================================================
    // Koşulları Kaydetme
    // ============================================================

    @Test
    fun `confirmAcceptance her iki onay ile başarıyla kaydedilir`() = runTest {
        coEvery {
            authRepository.acceptTermsAndPrivacy()
        } returns Result.Success(Unit)

        viewModel.onTermsAcceptedChanged(true)
        viewModel.onPrivacyAcceptedChanged(true)
        viewModel.confirmAcceptance()

        val state = viewModel.uiState.value
        assertThat(state.isAcceptanceConfirmed).isTrue()
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `confirmAcceptance onaysız çağrılırsa hata verilir`() = runTest {
        viewModel.confirmAcceptance()

        val state = viewModel.uiState.value
        assertThat(state.isAcceptanceConfirmed).isFalse()
        assertThat(state.errorMessage).isNotNull()
    }

    @Test
    fun `confirmAcceptance hata verdiğinde errorMessage ayarlanır`() = runTest {
        coEvery {
            authRepository.acceptTermsAndPrivacy()
        } returns Result.Error(Exception("Kayıt başarısız"))

        viewModel.onTermsAcceptedChanged(true)
        viewModel.onPrivacyAcceptedChanged(true)
        viewModel.confirmAcceptance()

        val state = viewModel.uiState.value
        assertThat(state.isAcceptanceConfirmed).isFalse()
        assertThat(state.errorMessage).isNotNull()
        assertThat(state.isLoading).isFalse()
    }
}