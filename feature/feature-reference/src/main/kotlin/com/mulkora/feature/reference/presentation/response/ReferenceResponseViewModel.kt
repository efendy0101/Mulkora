package com.mulkora.feature.reference.presentation.response

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.reference.domain.model.Reference
import com.mulkora.feature.reference.domain.repository.ReferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReferenceResponseUiState(
    val referenceId: String = "",
    val fromDisplayName: String = "",
    val fromUsername: String = "",
    val referenceContent: String = "",
    val responseContent: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface ReferenceResponseEvent {
    data object NavigateBack : ReferenceResponseEvent
}

/**
 * Referans cevap adımı ViewModel.
 *
 * CLAUDE.md §13:
 *   - Referans alıcısı (toUid), onaylanan referansa cevap yazabilir.
 *   - Cevap yönetici onayından geçtikten sonra yayımlanır.
 *   - Cevap backend'deki respondToReference Cloud Function ile oluşturulur.
 *   - Kullanıcı başka kullanıcı adına referans cevabı oluşturamaz
 *     (Firestore Security Rules + Cloud Function auth kontrolü ile güvence altında).
 *
 * docs/16-reference-system-state-machine.md: approved → response_pending_approval → response_approved
 */
@HiltViewModel
class ReferenceResponseViewModel @Inject constructor(
    private val referenceRepository: ReferenceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val referenceId: String = checkNotNull(savedStateHandle["referenceId"])

    private val _uiState = MutableStateFlow(ReferenceResponseUiState(referenceId = referenceId))
    val uiState: StateFlow<ReferenceResponseUiState> = _uiState.asStateFlow()

    private val _events = Channel<ReferenceResponseEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadReference()
    }

    private fun loadReference() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            referenceRepository.getReferenceById(referenceId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val ref: Reference? = result.data
                        if (ref != null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    fromDisplayName = ref.fromDisplayName,
                                    fromUsername = ref.fromUsername,
                                    referenceContent = ref.content,
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Referans bulunamadı.",
                                )
                            }
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message ?: "Referans yüklenemedi.",
                            )
                        }
                    }
                    is Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun onResponseContentChanged(text: String) {
        if (text.length <= MAX_RESPONSE_LENGTH) {
            _uiState.update { it.copy(responseContent = text, errorMessage = null) }
        }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.responseContent.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Cevap metni boş olamaz.") }
            return
        }
        if (state.responseContent.length < MIN_RESPONSE_LENGTH) {
            _uiState.update {
                it.copy(errorMessage = "Cevap en az $MIN_RESPONSE_LENGTH karakter olmalıdır.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = referenceRepository.respondToReference(
                referenceId = referenceId,
                responseContent = state.responseContent.trim(),
            )
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    _events.send(ReferenceResponseEvent.NavigateBack)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message ?: "Cevap gönderilemedi. Lütfen tekrar deneyin.",
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MIN_RESPONSE_LENGTH = 10
        const val MAX_RESPONSE_LENGTH = 1000
    }
}