package com.mulkora.feature.reference.presentation.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.reference.domain.model.ReferenceType
import com.mulkora.feature.reference.domain.model.RelationshipType
import com.mulkora.feature.reference.domain.repository.ReferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Referans oluşturma ViewModel.
 * CLAUDE.md §13: Referans başlatma Cloud Functions üzerinden yapılır.
 * initiateReference CF'ı kullanılır: toUid, referenceType, rating, content,
 * relationshipType, propertyAddress, startDate, endDate gerektirir.
 */
@HiltViewModel
class ReferenceCreateViewModel @Inject constructor(
    private val referenceRepository: ReferenceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val toUid: String = checkNotNull(savedStateHandle["toUid"])

    private val _uiState = MutableStateFlow(ReferenceCreateUiState(toUid = toUid))
    val uiState: StateFlow<ReferenceCreateUiState> = _uiState.asStateFlow()

    fun onReferenceTypeSelected(type: ReferenceType) {
        _uiState.update { it.copy(referenceType = type) }
    }

    fun onRelationshipTypeSelected(type: RelationshipType) {
        _uiState.update { it.copy(relationshipType = type) }
    }

    fun onRatingChanged(rating: Int) {
        _uiState.update { it.copy(rating = rating.coerceIn(1, 5)) }
    }

    fun onContentChanged(content: String) {
        _uiState.update { it.copy(content = content, contentError = null) }
    }

    fun onPropertyAddressChanged(address: String) {
        _uiState.update { it.copy(propertyAddress = address) }
    }

    fun onStartDateChanged(date: String) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun onEndDateChanged(date: String) {
        _uiState.update { it.copy(endDate = date) }
    }

    fun onSubmit(onSuccess: (referenceId: String) -> Unit) {
        val state = _uiState.value

        // Validasyon
        if (state.content.length < 20) {
            _uiState.update {
                it.copy(contentError = "Referans içeriği en az 20 karakter olmalıdır.")
            }
            return
        }
        if (state.referenceType == null) {
            _uiState.update { it.copy(generalError = "Referans türü seçiniz.") }
            return
        }
        if (state.relationshipType == null) {
            _uiState.update { it.copy(generalError = "İlişki türü seçiniz.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            val result = referenceRepository.initiateReference(
                toUid = state.toUid,
                referenceType = state.referenceType,
                rating = state.rating,
                content = state.content,
                relationshipType = state.relationshipType,
                propertyAddress = state.propertyAddress.ifBlank { null },
                startDate = state.startDate.ifBlank { null },
                endDate = state.endDate.ifBlank { null }
            )
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess(result.data)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = result.message ?: "Referans gönderilemedi."
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }
}

data class ReferenceCreateUiState(
    val toUid: String = "",
    val referenceType: ReferenceType? = null,
    val relationshipType: RelationshipType? = null,
    val rating: Int = 5,
    val content: String = "",
    val propertyAddress: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val isLoading: Boolean = false,
    val contentError: String? = null,
    val generalError: String? = null
)