package com.mulkora.feature.reference.presentation.write

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.reference.domain.model.RelationshipType
import com.mulkora.feature.reference.domain.repository.ReferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReferenceWriteUiState(
    val referenceId: String = "",
    val toDisplayName: String = "",
    val toUsername: String = "",
    val commentText: String = "",
    val relationshipType: RelationshipType = RelationshipType.FORMER_TENANT,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface ReferenceWriteEvent {
    data object NavigateToList : ReferenceWriteEvent
}

/**
 * Referans yazma adımı ViewModel.
 * CLAUDE.md §13: Referans state machine — awaiting_confirmation → preparing → pending_approval
 * Kullanıcı referans metnini yazar ve submitReference CF tetiklenir.
 * İçerik filtresi Cloud Function tarafında uygulanır.
 */
@HiltViewModel
class ReferenceWriteViewModel @Inject constructor(
    private val referenceRepository: ReferenceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val referenceId: String = checkNotNull(savedStateHandle["referenceId"])

    private val _uiState = MutableStateFlow(ReferenceWriteUiState(referenceId = referenceId))
    val uiState: StateFlow<ReferenceWriteUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<ReferenceWriteEvent?>(null)
    val events: StateFlow<ReferenceWriteEvent?> = _events.asStateFlow()

    init {
        loadReference()
    }

    private fun loadReference() {
        viewModelScope.launch {
            referenceRepository.getReferenceById(referenceId).collect { result ->
                if (result is Result.Success) {
                    result.data?.let { ref ->
                        _uiState.update {
                            it.copy(
                                toDisplayName = ref.toDisplayName,
                                toUsername = ref.toUsername,
                            )
                        }
                    }
                }
            }
        }
    }

    fun onCommentChanged(text: String) {
        if (text.length <= 1000) {
            _uiState.update { it.copy(commentText = text) }
        }
    }

    fun onRelationshipTypeChanged(type: RelationshipType) {
        _uiState.update { it.copy(relationshipType = type) }
    }

    /**
     * Referansı gönderir (submitReference Cloud Function).
     * CLAUDE.md: İçerik filtresi CF tarafında uygulanır; istemci doğrudan Firestore'a yazmaz.
     * pending_approval durumuna geçilir, moderasyon beklenir.
     */
    fun submitReference() {
        val state = _uiState.value
        if (state.commentText.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Lütfen bir referans metni girin.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = referenceRepository.submitReference(
                referenceId = referenceId,
                comment = state.commentText.trim(),
                relationshipType = state.relationshipType,
            )
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    _events.value = ReferenceWriteEvent.NavigateToList
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