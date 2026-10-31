package com.mulkora.feature.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.core.common.result.fold
import com.mulkora.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val displayName: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val displayNameError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistered: Boolean = false,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null) }
    }

    fun onDisplayNameChanged(value: String) {
        _uiState.update { it.copy(displayName = value, displayNameError = null) }
    }

    fun onRegister() {
        val state = _uiState.value

        val emailError = when {
            state.email.isBlank() -> "E-posta zorunludur."
            !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches() -> "Geçerli bir e-posta girin."
            else -> null
        }

        val displayNameError = when {
            state.displayName.isBlank() -> "Ad Soyad zorunludur."
            state.displayName.trim().length < 2 -> "Ad Soyad en az 2 karakter olmalıdır."
            state.displayName.trim().length > 50 -> "Ad Soyad en fazla 50 karakter olabilir."
            else -> null
        }

        val passwordError = when {
            state.password.isBlank() -> "Şifre zorunludur."
            state.password.length < 8 -> "Şifre en az 8 karakter olmalıdır."
            !state.password.any { it.isUpperCase() } -> "Şifre en az bir büyük harf içermelidir."
            !state.password.any { it.isDigit() } -> "Şifre en az bir rakam içermelidir."
            else -> null
        }

        val confirmPasswordError = when {
            state.confirmPassword.isBlank() -> "Şifre tekrarı zorunludur."
            state.confirmPassword != state.password -> "Şifreler eşleşmiyor."
            else -> null
        }

        if (emailError != null || passwordError != null || confirmPasswordError != null || displayNameError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                    displayNameError = displayNameError,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.registerWithEmail(
                email = state.email.trim(),
                password = state.password,
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isRegistered = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Kayıt başarısız. Lütfen tekrar deneyin.",
                        )
                    }
                },
                onLoading = { /* No-op or handle if needed */ }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}