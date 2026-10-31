package com.mulkora.feature.contracts.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.contracts.domain.model.Contract
import com.mulkora.feature.contracts.domain.model.ContractStatus
import com.mulkora.feature.contracts.domain.model.PdfStatus
import com.mulkora.feature.contracts.domain.repository.ContractRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ARG_CONTRACT_ID = "contractId"

/**
 * ContractDetailScreen için ViewModel.
 *
 * Sözleşme onaylama, düzenleme talebi, PDF oluşturma ve kilitleme
 * işlemlerini yönetir.
 *
 * CLAUDE.md §14: Sözleşme akışı — taslak → taraf daveti → onaylama
 * → kilitleme → PDF üretimi → indirme.
 * CLAUDE.md §26: Sözleşmeler yalnızca taraflar ve yetkili yöneticiler
 * tarafından okunabilir; bu kontrol Firestore Security Rules'da da uygulanır.
 */
@HiltViewModel
class ContractDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ContractRepository,
) : ViewModel() {

    private val contractId: String = checkNotNull(savedStateHandle[ARG_CONTRACT_ID])

    private val _uiState = MutableStateFlow(ContractDetailUiState())
    val uiState: StateFlow<ContractDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ContractDetailEvent>()
    val events: SharedFlow<ContractDetailEvent> = _events.asSharedFlow()

    init {
        observeContract()
    }

    private fun observeContract() {
        repository.observeContract(contractId)
            .onEach { result ->
                _uiState.update { state ->
                    when (result) {
                        is Result.Loading -> state.copy(isLoading = true)
                        is Result.Success -> state.copy(
                            isLoading = false,
                            contract = result.data,
                            errorMessage = null,
                        )
                        is Result.Error -> state.copy(
                            isLoading = false,
                            errorMessage = result.message ?: "Sözleşme yüklenemedi.",
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun approveContract() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            when (val result = repository.approveContract(contractId)) {
                is Result.Success -> {
                    if (result.data) {
                        // Her iki taraf onayladıysa kilitleme başlatılır (Cloud Function tetikler)
                        _events.emit(ContractDetailEvent.ShowMessage("Sözleşme onaylandı. Kilitleme başlatıldı."))
                    } else {
                        _events.emit(ContractDetailEvent.ShowMessage("Onayınız kaydedildi. Diğer tarafın onayı bekleniyor."))
                    }
                }
                is Result.Error -> _events.emit(
                    ContractDetailEvent.ShowMessage(result.message ?: "Onay işlemi başarısız.")
                )
                else -> Unit
            }
            _uiState.update { it.copy(isActionLoading = false) }
        }
    }

    fun requestEdit(note: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            when (val result = repository.requestEdit(contractId, note)) {
                is Result.Success -> _events.emit(ContractDetailEvent.ShowMessage("Düzenleme talebi gönderildi."))
                is Result.Error -> _events.emit(
                    ContractDetailEvent.ShowMessage(result.message ?: "Düzenleme talebi gönderilemedi.")
                )
                else -> Unit
            }
            _uiState.update { it.copy(isActionLoading = false) }
        }
    }

    fun generatePdf() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            when (val result = repository.generatePdf(contractId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isActionLoading = false) }
                    _events.emit(ContractDetailEvent.OpenPdfUrl(result.data))
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isActionLoading = false) }
                    _events.emit(
                        ContractDetailEvent.ShowMessage(result.message ?: "PDF oluşturulamadı.")
                    )
                }
                else -> _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun showEditDialog() {
        _uiState.update { it.copy(showEditRequestDialog = true) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditRequestDialog = false) }
    }
}

data class ContractDetailUiState(
    val isLoading: Boolean = true,
    val isActionLoading: Boolean = false,
    val contract: Contract? = null,
    val errorMessage: String? = null,
    val showEditRequestDialog: Boolean = false,
) {
    /** Mevcut kullanıcı onaylayabilir mi? (durum uygunsa ve PDF hazır değilse) */
    val canApprove: Boolean
        get() = contract?.status in listOf(
            ContractStatus.AWAITING_APPROVAL,
            ContractStatus.BOTH_APPROVED,
        )

    /** Düzenleme talebi gönderilebilir mi? */
    val canRequestEdit: Boolean
        get() = contract?.status in listOf(
            ContractStatus.AWAITING_APPROVAL,
        )

    /** PDF oluşturma butonu görünür mü? */
    val canGeneratePdf: Boolean
        get() = contract?.status == ContractStatus.LOCKED &&
                contract.pdfStatus != PdfStatus.READY
}

sealed class ContractDetailEvent {
    data class ShowMessage(val message: String) : ContractDetailEvent()
    data class OpenPdfUrl(val url: String) : ContractDetailEvent()
}