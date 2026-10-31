package com.mulkora.feature.reference.presentation.recognition

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

data class ReferenceRecognitionUiState(
    val referenceId: String = "",
    val fromDisplayName: String = "",
    val fromUsername: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface ReferenceRecognitionEvent {
    data object NavigateBack : ReferenceRecognitionEvent
    data object NavigateToResponse : ReferenceRecognitionEvent
}

/**
 * Referans tanıma adımı ViewModel.
 * CLAUDE.md §13: Referans state machine — invite_sent → awaiting_confirmation
 * Kullanıcı "Tanıyorum" ya da "Tanımıyorum" seçimi yapar.
 * acceptReference veya rejectReference Cloud Function çağrılır.
 */
@HiltViewModel
class ReferenceRecognitionViewModel @Inject constructor(
    private val referenceRepository: ReferenceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val referenceId: String = checkNotNull(savedStateHandle["referenceId"])

    private val _uiState = MutableStateFlow(ReferenceRecognitionUiState(referenceId = referenceId))
    val uiState: StateFlow<ReferenceRecognitionUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<ReferenceRecognitionEvent?>(null)
    val events: StateFlow<ReferenceRecognitionEvent?> = _events.asStateFlow()

    init {
        loadReference()
    }

    private fun loadReference() {
        referenceRepository.getReferenceById(referenceId)
            .let { flow ->
                viewModelScope.launch {
                    flow.collect { result ->
                        if (result is Result.Success) {
                            result.data?.let { ref ->
                                _uiState.update {
                                    it.copy(
                                        fromDisplayName = ref.fromDisplayName,
                                        fromUsername = ref.fromUsername,
                                    )
                                }
                            }
                        }
                    }
                }
            }
    }

    /** Kullanıcı referans veren kişiyi tanıdığını onaylar → awaiting_confirmation durumuna geçer. */
    fun acceptReference() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = referenceRepository.acceptReference(referenceId)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    _events.value = ReferenceRecognitionEvent.NavigateToResponse
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

    /** Kullanıcı referans veren kişiyi tanımadığını belirtir → not_recognized durumuna geçer. */
    fun rejectRecognition() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = referenceRepository.rejectReference(referenceId)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.value = ReferenceRecognitionEvent.NavigateBack
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