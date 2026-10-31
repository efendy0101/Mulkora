package com.mulkora.feature.reference.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mulkora.core.common.model.ReferenceStatus
import com.mulkora.core.common.result.Result
import com.mulkora.feature.reference.domain.model.Reference
import com.mulkora.feature.reference.domain.repository.ReferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Referans detay ViewModel.
 *
 * CLAUDE.md §13:
 * - Referans alıcısı (toUid) onaylı referansa cevap verebilir (response == null ise).
 * - Referans alıcısı (toUid) onaylı referansa itiraz edebilir.
 * - "Cevap Ver" ve "İtiraz Et" butonları yalnızca mevcut kullanıcı toUid ise gösterilir.
 *
 * docs/16-reference-system-state-machine.md: approved durumundaki referanslarda
 * cevap ve itiraz aksiyonları tetiklenebilir.
 */
@HiltViewModel
class ReferenceDetailViewModel @Inject constructor(
    private val referenceRepository: ReferenceRepository,
    private val firebaseAuth: FirebaseAuth,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val referenceId: String = checkNotNull(savedStateHandle["referenceId"])

    private val _uiState = MutableStateFlow(ReferenceDetailUiState())
    val uiState: StateFlow<ReferenceDetailUiState> = _uiState.asStateFlow()

    init {
        observeReference()
    }

    private fun observeReference() {
        referenceRepository.getReferenceById(referenceId)
            .onEach { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> {
                        val reference = result.data
                        val currentUid = firebaseAuth.currentUser?.uid
                        val isRecipient = reference?.toUid == currentUid
                        val isApproved = reference?.status == ReferenceStatus.APPROVED

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                reference = reference,
                                // "Cevap Ver" butonu: alıcı + onaylı referans + henüz cevap yok
                                canRespond = isRecipient && isApproved && reference?.response == null,
                                // "İtiraz Et" butonu: alıcı + onaylı referans
                                canAppeal = isRecipient && isApproved,
                            )
                        }
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class ReferenceDetailUiState(
    val reference: Reference? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /**
     * Mevcut kullanıcı bu referansın alıcısıysa (toUid) ve referans onaylıysa
     * ve henüz bir cevap yazılmamışsa true.
     */
    val canRespond: Boolean = false,
    /**
     * Mevcut kullanıcı bu referansın alıcısıysa (toUid) ve referans onaylıysa true.
     */
    val canAppeal: Boolean = false,
)