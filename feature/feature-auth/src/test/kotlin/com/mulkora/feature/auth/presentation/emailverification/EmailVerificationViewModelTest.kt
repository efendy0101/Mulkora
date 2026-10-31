package com.mulkora.feature.auth.presentation.emailverification

import app.cash.turbine.test
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
 * EmailVerificationViewModelTest — E-posta doğrulama senaryoları.
 *
 * CLAUDE.md §2: Kayıt sonrası e-posta doğrulama zorunludur.
 *   - Yeniden gönderme: 60 sn cooldown
 *   - reloadUser() ile token senkronizasyonu
 * docs/21-testing-strategy.md — UT-AUTH kapsamı.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: EmailVerificationViewModel

    @Before
    fun setup() {
        authRepository = mockk()
        viewModel = EmailVerificationViewModel(authRepository)
    }

    // ============================================================
    // Başlangıç Durumu
    // ============================================================

    @Test
    fun `başlangıçta varsayılan değerler doğru`() {
        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.isEmailVerified).isFalse()
        assertThat(state.errorMessage).isNull()
        assertThat(state.isResendCooldown).isFalse()
    }

    // ============================================================
    // E-posta Yeniden Gönderme
    // ============================================================

    @Test
    fun `resendVerificationEmail başarıyla gönderildiğinde isResendCooldown true olur`() = runTest {
        coEvery {
            authRepository.sendEmailVerification()
        } returns Result.Success(Unit)

        viewModel.uiState.test {
            awaitItem() // başlangıç

            viewModel.resendVerificationEmail()
            val state = awaitItem()
            assertThat(state.isResendCooldown).isTrue()
            assertThat(state.errorMessage).isNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resendVerificationEmail başarısız olduğunda errorMessage ayarlanır`() = runTest {
        coEvery {
            authRepository.sendEmailVerification()
        } returns Result.Error(Exception("Gönderim hatası"))

        viewModel.uiState.test {
            awaitItem()

            viewModel.resendVerificationEmail()
            val state = awaitItem()
            assertThat(state.errorMessage).isNotNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // E-posta Doğrulama Kontrolü (reloadUser)
    // ============================================================

    @Test
    fun `checkEmailVerification doğrulanmışsa isEmailVerified true olur`() = runTest {
        coEvery {
            authRepository.reloadUser()
        } returns Result.Success(Unit)
        coEvery {
            authRepository.isEmailVerified()
        } returns true

        viewModel.uiState.test {
            awaitItem()

            viewModel.checkEmailVerification()
            val state = awaitItem()
            assertThat(state.isEmailVerified).isTrue()
            assertThat(state.errorMessage).isNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `checkEmailVerification doğrulanmamışsa isEmailVerified false kalır`() = runTest {
        coEvery {
            authRepository.reloadUser()
        } returns Result.Success(Unit)
        coEvery {
            authRepository.isEmailVerified()
        } returns false

        viewModel.uiState.test {
            awaitItem()

            viewModel.checkEmailVerification()
            // Doğrulanmamış — state değişmez veya false kalır
            val state = awaitItem()
            assertThat(state.isEmailVerified).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `checkEmailVerification hata verdiğinde errorMessage ayarlanır`() = runTest {
        coEvery {
            authRepository.reloadUser()
        } returns Result.Error(Exception("Yenileme başarısız"))

        viewModel.uiState.test {
            awaitItem()

            viewModel.checkEmailVerification()
            val state = awaitItem()
            assertThat(state.errorMessage).isNotNull()

            cancelAndIgnoreRemainingEvents()
        }
    }
}