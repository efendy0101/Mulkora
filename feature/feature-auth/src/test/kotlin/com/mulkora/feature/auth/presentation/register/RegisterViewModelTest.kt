package com.mulkora.feature.auth.presentation.register

import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.auth.domain.repository.AuthRepository
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
 * RegisterViewModel birim testleri.
 *
 * CLAUDE.md §28: Her ViewModel için en az 5 test senaryosu.
 * docs/21-testing-strategy.md — UT-AUTH-010 ile UT-AUTH-018.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        authRepository = mockk()
        viewModel = RegisterViewModel(authRepository)
    }

    // ─── UT-AUTH-010: Başlangıç durumu ───────────────────────────────────────

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertTrue(state.email.isEmpty())
        assertTrue(state.password.isEmpty())
        assertTrue(state.confirmPassword.isEmpty())
        assertTrue(state.displayName.isEmpty())
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.confirmPasswordError)
        assertNull(state.displayNameError)
        assertFalse(state.isLoading)
        assertFalse(state.isRegistered)
        assertNull(state.errorMessage)
    }

    // ─── UT-AUTH-011: Boş e-posta doğrulama ─────────────────────────────────

    @Test
    fun `register with empty email sets emailError`() = runTest {
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("Test1234!")
        viewModel.onConfirmPasswordChanged("Test1234!")
        viewModel.onDisplayNameChanged("Test User")
        viewModel.onRegister()

        val state = viewModel.uiState.value
        assertNotNull(state.emailError)
        assertFalse(state.isLoading)
    }

    // ─── UT-AUTH-012: Geçersiz e-posta doğrulama ─────────────────────────────

    @Test
    fun `register with invalid email sets emailError`() = runTest {
        viewModel.onEmailChanged("invalid-email")
        viewModel.onPasswordChanged("Test1234!")
        viewModel.onConfirmPasswordChanged("Test1234!")
        viewModel.onDisplayNameChanged("Test User")
        viewModel.onRegister()

        assertNotNull(viewModel.uiState.value.emailError)
    }

    // ─── UT-AUTH-013: Çok kısa şifre doğrulama ───────────────────────────────

    @Test
    fun `register with short password sets passwordError`() = runTest {
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("abc")   // < 8 karakter
        viewModel.onConfirmPasswordChanged("abc")
        viewModel.onDisplayNameChanged("Test User")
        viewModel.onRegister()

        assertNotNull(viewModel.uiState.value.passwordError)
    }

    // ─── UT-AUTH-014: Şifreler eşleşmiyor ───────────────────────────────────

    @Test
    fun `register with mismatched passwords sets confirmPasswordError`() = runTest {
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("Test1234!")
        viewModel.onConfirmPasswordChanged("Different1!")
        viewModel.onDisplayNameChanged("Test User")
        viewModel.onRegister()

        assertNotNull(viewModel.uiState.value.confirmPasswordError)
    }

    // ─── UT-AUTH-015: Çok kısa display name ─────────────────────────────────

    @Test
    fun `register with single char display name sets displayNameError`() = runTest {
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("Test1234!")
        viewModel.onConfirmPasswordChanged("Test1234!")
        viewModel.onDisplayNameChanged("A")   // < 2 karakter
        viewModel.onRegister()

        assertNotNull(viewModel.uiState.value.displayNameError)
    }

    // ─── UT-AUTH-016: E-posta değişimi emailError'u temizler ─────────────────

    @Test
    fun `onEmailChanged clears emailError`() = runTest {
        viewModel.onEmailChanged("invalid")
        viewModel.onRegister()
        assertNotNull(viewModel.uiState.value.emailError)

        viewModel.onEmailChanged("valid@example.com")
        assertNull(viewModel.uiState.value.emailError)
    }

    // ─── UT-AUTH-017: Şifre değişimi passwordError'u temizler ───────────────

    @Test
    fun `onPasswordChanged clears passwordError`() = runTest {
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("abc")
        viewModel.onRegister()
        assertNotNull(viewModel.uiState.value.passwordError)

        viewModel.onPasswordChanged("Test1234!")
        assertNull(viewModel.uiState.value.passwordError)
    }

    // ─── UT-AUTH-018: Büyük harf veya rakam içermeyen şifre ─────────────────

    @Test
    fun `register with password without uppercase sets passwordError`() = runTest {
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("testtest1")  // büyük harf yok
        viewModel.onConfirmPasswordChanged("testtest1")
        viewModel.onDisplayNameChanged("Test User")
        viewModel.onRegister()

        assertNotNull(viewModel.uiState.value.passwordError)
    }
}