package com.mulkora.feature.auth.presentation.splash

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.auth.domain.repository.AuthRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * SplashViewModelTest — Splash / Oturum kontrolü senaryoları.
 *
 * CLAUDE.md §1: Uygulama açılışında oturum durumu kontrol edilir.
 *   - Oturum yoksa → giriş/kayıt
 *   - E-posta doğrulanmamışsa → e-posta doğrulama
 *   - Rol seçilmemişse → rol seçim
 *   - Kullanıcı adı alınmamışsa → kullanıcı adı seçim
 *   - Koşullar onaylanmamışsa → koşul kabul
 *   - Her şey tamam → ana ekran
 * docs/21-testing-strategy.md — UT-AUTH kapsamı.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: SplashViewModel

    @Before
    fun setup() {
        authRepository = mockk()
    }

    // ============================================================
    // Başlangıç Durumu
    // ============================================================

    @Test
    fun `başlangıçta navigasyon hedefi null`() {
        every { authRepository.currentUser } returns flowOf(null)
        viewModel = SplashViewModel(authRepository)

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isTrue()
        assertThat(state.navigationTarget).isNull()
    }

    // ============================================================
    // Oturum Yok
    // ============================================================

    @Test
    fun `oturum açılmamışsa navigasyon hedefi giriş ekranı olur`() = runTest {
        every { authRepository.currentUser } returns flowOf(null)
        viewModel = SplashViewModel(authRepository)

        viewModel.uiState.test {
            // İlk state: loading
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            // Navigasyon state
            val navState = awaitItem()
            assertThat(navState.navigationTarget).isEqualTo(SplashNavigationTarget.LOGIN)
            assertThat(navState.isLoading).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Oturum Var — Akış Kontrolü
    // ============================================================

    @Test
    fun `oturum var ve e-posta doğrulanmamışsa email doğrulama ekranına yönlendirir`() = runTest {
        every { authRepository.currentUser } returns flowOf(
            mockk {
                every { isEmailVerified } returns false
                every { uid } returns "user_001"
            }
        )
        viewModel = SplashViewModel(authRepository)

        viewModel.uiState.test {
            awaitItem() // loading

            val navState = awaitItem()
            assertThat(navState.navigationTarget)
                .isEqualTo(SplashNavigationTarget.EMAIL_VERIFICATION)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `e-posta doğrulandı rol seçilmemişse rol seçim ekranına yönlendirir`() = runTest {
        every { authRepository.currentUser } returns flowOf(
            mockk {
                every { isEmailVerified } returns true
                every { uid } returns "user_001"
                every { role } returns null
            }
        )
        viewModel = SplashViewModel(authRepository)

        viewModel.uiState.test {
            awaitItem()

            val navState = awaitItem()
            assertThat(navState.navigationTarget)
                .isEqualTo(SplashNavigationTarget.ROLE_SELECTION)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `e-posta doğrulandı rol var kullanıcı adı yoksa kullanıcı adı seçim ekranına yönlendirir`() = runTest {
        every { authRepository.currentUser } returns flowOf(
            mockk {
                every { isEmailVerified } returns true
                every { uid } returns "user_001"
                every { role } returns "tenant"
                every { username } returns null
            }
        )
        viewModel = SplashViewModel(authRepository)

        viewModel.uiState.test {
            awaitItem()

            val navState = awaitItem()
            assertThat(navState.navigationTarget)
                .isEqualTo(SplashNavigationTarget.USERNAME_SELECTION)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tüm adımlar tamamlandıysa ana ekrana yönlendirir`() = runTest {
        every { authRepository.currentUser } returns flowOf(
            mockk {
                every { isEmailVerified } returns true
                every { uid } returns "user_001"
                every { role } returns "tenant"
                every { username } returns "mevcut_kullanici"
                every { hasAcceptedTerms } returns true
            }
        )
        viewModel = SplashViewModel(authRepository)

        viewModel.uiState.test {
            awaitItem()

            val navState = awaitItem()
            assertThat(navState.navigationTarget).isEqualTo(SplashNavigationTarget.HOME)

            cancelAndIgnoreRemainingEvents()
        }
    }
}