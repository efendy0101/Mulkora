package com.mulkora.feature.reference.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.auth.domain.repository.AuthRepository
import com.mulkora.feature.reference.domain.model.Reference
import com.mulkora.feature.reference.domain.repository.ReferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Referans listesi ViewModel.
 * CLAUDE.md Bölüm 5: Referans sistemi — kiracı/ev sahibi geçmişi doğrulama.
 * Referanslar state machine'e göre yönetilir (docs/16-reference-system-state-machine.md).
 *
 * Metodlar:
 *  - RECEIVED : getReferencesForUser(myUid)
 *  - GIVEN    : getReferencesGivenByUser(myUid)
 *  - PENDING  : getPendingReferenceRequests()
 */
data class ReferenceListUiState(
    val references: List<Reference> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedTab: ReferenceTab = ReferenceTab.RECEIVED,
)

enum class ReferenceTab {
    RECEIVED,  // Alınan referanslar (bana verilmiş)
    GIVEN,     // Verdiğim referanslar
    PENDING,   // Yanıt bekleyenler (bana gelen bekleyen istekler)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReferenceListViewModel @Inject constructor(
    private val referenceRepository: ReferenceRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReferenceListUiState())
    val uiState: StateFlow<ReferenceListUiState> = _uiState.asStateFlow()

    init {
        loadReferences()
    }

    fun onTabSelected(tab: ReferenceTab) {
        _uiState.update { it.copy(selectedTab = tab, references = emptyList(), isLoading = true) }
        loadReferences()
    }

    private fun loadReferences() {
        viewModelScope.launch {
            // AuthRepository.currentUser bir Flow'dur; first() ile anlık değeri alıyoruz
            val currentUid = authRepository.currentUser.first()?.uid
            if (currentUid == null) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Oturum açmanız gerekiyor.")
                }
                return@launch
            }

            val flow = when (_uiState.value.selectedTab) {
                ReferenceTab.RECEIVED -> referenceRepository.getReferencesForUser(currentUid)
                ReferenceTab.GIVEN    -> referenceRepository.getReferencesGivenByUser(currentUid)
                ReferenceTab.PENDING  -> referenceRepository.getPendingReferenceRequests()
            }

            flow.collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(references = result.data, isLoading = false)
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.exception.message ?: "Referanslar yüklenemedi.",
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}