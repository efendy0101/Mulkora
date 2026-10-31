package com.mulkora.feature.auth.presentation.login

import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.auth.domain.model.AuthUser
import com.mulkora.feature.auth.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * LoginViewModel birim testleri.
 *
 * CLAUDE.md §28: Test kapsamı — her ViewModel için en az 5 senaryo.
 * docs/21-testing-strategy.md — UT-AUTH-001 ile UT-AUTH-005.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        authRepository = mockk()
        viewModel = LoginViewModel(authRepository)
    }

    // ─── UT-AUTH-001: Başlangıç durumu ────────────────────────────────────────

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.generalError)
        assertFalse(state.isLoading)
        assertNull(state.navigationEvent)
    }

    // ─── UT-AUTH-002: E-posta ve şifre doğrulama ─────────────────────────────

    @Test
    fun `onSignInClick with empty email sets emailError`() = runTest {
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("password123")
        viewModel.onSignInClick()

        val state = viewModel.uiState.value
        assertNotNull(state.emailError)
        assertNull(state.passwordError)
    }

    @Test
    fun `onSignInClick with invalid email sets emailError`() = runTest {
        viewModel.onEmailChange("not-an-email")
        viewModel.onPasswordChange("password123")
        viewModel.onSignInClick()

        val state = viewModel.uiState.value
        assertNotNull(state.emailError)
    }

    @Test
    fun `onSignInClick with empty password sets passwordError`() = runTest {
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("")
        viewModel.onSignInClick()

        val state = viewModel.uiState.value
        assertNull(state.emailError)
        assertNotNull(state.passwordError)
    }

    // ─── UT-AUTH-003: Başarılı giriş ─────────────────────────────────────────

    @Test
    fun `successful login with verified email triggers NavigateToHome`() = runTest {
        val mockUser = AuthUser(
            uid = "user123",
            email = "test@example.com",
            isEmailVerified = true,
            displayName = "Test User",
            photoUrl = null
        )
        coEvery {
            authRepository.signInWithEmail("test@example.com", "password123")
        } returns Result.Success(mockUser)

        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onSignInClick()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(LoginUiState.NavigationEvent.NavigateToHome, state.navigationEvent)
    }

    // ─── UT-AUTH-004: E-posta doğrulanmamış ──────────────────────────────────

    @Test
    fun `login with unverified email triggers EmailNotVerified`() = runTest {
        val mockUser = AuthUser(
            uid = "user123",
            email = "test@example.com",
            isEmailVerified = false,
            displayName = "Test User",
            photoUrl = null
        )
        coEvery {
            authRepository.signInWithEmail("test@example.com", "password123")
        } returns Result.Success(mockUser)

        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onSignInClick()

        val state = viewModel.uiState.value
        assertEquals(LoginUiState.NavigationEvent.EmailNotVerified, state.navigationEvent)
    }

    // ─── UT-AUTH-005: Başarısız giriş ────────────────────────────────────────

    @Test
    fun `failed login sets generalError`() = runTest {
        coEvery {
            authRepository.signInWithEmail("test@example.com", "wrongpass")
        } returns Result.Failure(Exception("Kullanıcı adı veya şifre hatalı."))

        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("wrongpass")
        viewModel.onSignInClick()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.generalError)
        assertNull(state.navigationEvent)
    }

    // ─── UT-AUTH-006: Google giriş başarısız — null token ────────────────────

    @Test
    fun `google sign in with null token sets generalError`() = runTest {
        viewModel.onGoogleSignInResult(null)

        val state = viewModel.uiState.value
        assertNotNull(state.generalError)
        assertNull(state.navigationEvent)
    }

    // ─── UT-AUTH-007: Google giriş başarılı ──────────────────────────────────

    @Test
    fun `google sign in success triggers NavigateToHome`() = runTest {
        val mockUser = AuthUser(
            uid = "google123",
            email = "google@gmail.com",
            isEmailVerified = true,
            displayName = "Google User",
            photoUrl = "https://example.com/photo.jpg"
        )
        coEvery {
            authRepository.signInWithGoogle("valid_google_token")
        } returns Result.Success(mockUser)

        viewModel.onGoogleSignInResult("valid_google_token")

        val state = viewModel.uiState.value
        assertEquals(LoginUiState.NavigationEvent.NavigateToHome, state.navigationEvent)
    }

    // ─── UT-AUTH-008: Navigasyon event sıfırlama ─────────────────────────────

    @Test
    fun `onNavigationHandled clears navigationEvent`() = runTest {
        val mockUser = AuthUser(
            uid = "user123",
            email = "test@example.com",
            isEmailVerified = true,
            displayName = "Test User",
            photoUrl = null
        )
        coEvery {
            authRepository.signInWithEmail("test@example.com", "password123")
        } returns Result.Success(mockUser)

        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onSignInClick()

        // Önce event set edildi
        assertNotNull(viewModel.uiState.value.navigationEvent)

        // Sonra sıfırlandı
        viewModel.onNavigationHandled()
        assertNull(viewModel.uiState.value.navigationEvent)
    }

    // ─── UT-AUTH-009: E-posta değişimi email hatasını temizler ───────────────

    @Test
    fun `onEmailChange clears emailError`() = runTest {
        // Önce hata oluştur
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("password123")
        viewModel.onSignInClick()
        assertNotNull(viewModel.uiState.value.emailError)

        // Sonra e-posta değiştir — hata temizlenmeli
        viewModel.onEmailChange("new@example.com")
        assertNull(viewModel.uiState.value.emailError)
    }
}