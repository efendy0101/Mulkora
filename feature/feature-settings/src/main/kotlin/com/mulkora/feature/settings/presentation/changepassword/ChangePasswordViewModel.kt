package com.mulkora.feature.settings.presentation.changepassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Şifre değiştirme ViewModel.
 * CLAUDE.md §3: Şifre değiştirme Firebase Auth üzerinden yapılır.
 * - Re-authentication gereklidir (mevcut şifre doğrulanır).
 * - Yeni şifre: min 8 karakter, en az bir rakam ve harf.
 */
@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onCurrentPasswordChange(value: String) {
        _uiState.update { it.copy(currentPassword = value, error = null) }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, error = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, error = null) }
    }

    fun onChangePasswordClicked(onSuccess: () -> Unit) {
        val state = _uiState.value
        val error = validate(state)
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.changePassword(
                currentPassword = state.currentPassword,
                newPassword = state.newPassword
            )) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message ?: "Şifre değiştirilemedi.")
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun validate(state: ChangePasswordUiState): String? {
        if (state.currentPassword.isBlank()) return "Mevcut şifre boş olamaz."
        if (state.newPassword.length < 8) return "Yeni şifre en az 8 karakter olmalıdır."
        if (!state.newPassword.any { it.isDigit() } || !state.newPassword.any { it.isLetter() }) {
            return "Şifre en az bir harf ve bir rakam içermelidir."
        }
        if (state.newPassword != state.confirmPassword) return "Şifreler eşleşmiyor."
        if (state.currentPassword == state.newPassword) return "Yeni şifre mevcut şifre ile aynı olamaz."
        return null
    }
}

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)