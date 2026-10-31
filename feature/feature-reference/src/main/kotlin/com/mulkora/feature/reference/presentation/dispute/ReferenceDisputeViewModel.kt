package com.mulkora.feature.reference.presentation.dispute

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.reference.domain.repository.ReferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReferenceDisputeUiState(
    val referenceId: String = "",
    val disputeReason: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface ReferenceDisputeEvent {
    data object NavigateBack : ReferenceDisputeEvent
}

/**
 * Referans itiraz ViewModel.
 * CLAUDE.md §13: Referans state machine — approved → disputed
 * disputeReference Cloud Function üzerinden itiraz başlatılır.
 * KURAL: İstemci doğrudan Firestore'a yazmaz.
 */
@HiltViewModel
class ReferenceDisputeViewModel @Inject constructor(
    private val referenceRepository: ReferenceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val referenceId: String = checkNotNull(savedStateHandle["referenceId"])

    private val _uiState = MutableStateFlow(ReferenceDisputeUiState(referenceId = referenceId))
    val uiState: StateFlow<ReferenceDisputeUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<ReferenceDisputeEvent?>(null)
    val events: StateFlow<ReferenceDisputeEvent?> = _events.asStateFlow()

    fun onReasonChanged(text: String) {
        if (text.length <= 500) {
            _uiState.update { it.copy(disputeReason = text) }
        }
    }

    fun submitDispute() {
        val reason = _uiState.value.disputeReason
        if (reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Lütfen itiraz gerekçenizi girin.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = referenceRepository.disputeReference(
                referenceId = referenceId,
                reason = reason.trim(),
            )
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    _events.value = ReferenceDisputeEvent.NavigateBack
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message ?: "Bir hata oluştu.")
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun consumeEvent() {
        _events.value = null
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}