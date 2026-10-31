package com.mulkora.feature.settings.presentation.deleteaccount

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
 * Hesap silme ViewModel.
 * CLAUDE.md §3: Hesap silme işlemi Cloud Function üzerinden yapılır.
 * - Kullanıcı, onay metni girerek kastî olduğunu doğrular.
 * - Başarı durumunda oturum kapatılır ve giriş ekranına yönlendirilir.
 */
@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeleteAccountUiState())
    val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

    companion object {
        const val CONFIRMATION_TEXT = "HESABIMI SİL"
    }

    fun onConfirmationTextChange(value: String) {
        _uiState.update { it.copy(confirmationText = value, error = null) }
    }

    fun onDeleteClicked(onDeleted: () -> Unit) {
        val state = _uiState.value
        if (state.confirmationText.trim() != CONFIRMATION_TEXT) {
            _uiState.update { it.copy(error = "Lütfen '$CONFIRMATION_TEXT' yazın.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.deleteAccount()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onDeleted()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message ?: "Hesap silinemedi.")
                    }
                }
                is Result.Loading -> {}
            }
        }
    }
}

data class DeleteAccountUiState(
    val confirmationText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)