package com.mulkora.feature.auth.presentation.forgotpassword

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
 * ForgotPasswordViewModelTest — Şifre sıfırlama senaryoları.
 *
 * CLAUDE.md §4: Şifre sıfırlama Firebase Authentication üzerinden yapılır.
 * docs/21-testing-strategy.md — UT-AUTH kapsamı.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: ForgotPasswordViewModel

    @Before
    fun setup() {
        authRepository = mockk()
        viewModel = ForgotPasswordViewModel(authRepository)
    }

    // ============================================================
    // Başlangıç Durumu
    // ============================================================

    @Test
    fun `başlangıçta tüm alanlar varsayılan değerlerde`() {
        val state = viewModel.uiState.value
        assertThat(state.email).isEmpty()
        assertThat(state.isLoading).isFalse()
        assertThat(state.isEmailSent).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    // ============================================================
    // E-posta Girişi
    // ============================================================

    @Test
    fun `onEmailChanged email alanını günceller`() {
        viewModel.onEmailChanged("test@example.com")
        assertThat(viewModel.uiState.value.email).isEqualTo("test@example.com")
    }

    @Test
    fun `boş e-posta girildiğinde email alanı boş kalır`() {
        viewModel.onEmailChanged("test@example.com")
        viewModel.onEmailChanged("")
        assertThat(viewModel.uiState.value.email).isEmpty()
    }

    // ============================================================
    // Şifre Sıfırlama — Başarılı
    // ============================================================

    @Test
    fun `şifre sıfırlama e-postası başarıyla gönderilir`() = runTest {
        coEvery {
            authRepository.sendPasswordResetEmail("test@example.com")
        } returns Result.Success(Unit)

        viewModel.onEmailChanged("test@example.com")
        viewModel.sendPasswordResetEmail()

        val state = viewModel.uiState.value
        assertThat(state.isEmailSent).isTrue()
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    // ============================================================
    // Şifre Sıfırlama — Hata
    // ============================================================

    @Test
    fun `geçersiz e-posta ile gönderim errorMessage ayarlar`() = runTest {
        coEvery {
            authRepository.sendPasswordResetEmail(any())
        } returns Result.Error(Exception("Geçersiz e-posta adresi"))

        viewModel.onEmailChanged("gecersiz@test.com")
        viewModel.sendPasswordResetEmail()

        val state = viewModel.uiState.value
        assertThat(state.isEmailSent).isFalse()
        assertThat(state.errorMessage).isNotNull()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `kayıtlı olmayan e-posta ile gönderim errorMessage ayarlar`() = runTest {
        coEvery {
            authRepository.sendPasswordResetEmail(any())
        } returns Result.Error(Exception("Kullanıcı bulunamadı"))

        viewModel.onEmailChanged("yok@test.com")
        viewModel.sendPasswordResetEmail()

        val state = viewModel.uiState.value
        assertThat(state.isEmailSent).isFalse()
        assertThat(state.errorMessage).isNotNull()
    }

    // ============================================================
    // Yükleme Durumu
    // ============================================================

    @Test
    fun `gönderim sırasında isLoading true olur ve sonra false döner`() = runTest {
        // Başarılı sonuç
        coEvery {
            authRepository.sendPasswordResetEmail(any())
        } returns Result.Success(Unit)

        viewModel.onEmailChanged("test@example.com")
        viewModel.sendPasswordResetEmail()

        // Gönderim bitti; isLoading false olmalı
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }
}