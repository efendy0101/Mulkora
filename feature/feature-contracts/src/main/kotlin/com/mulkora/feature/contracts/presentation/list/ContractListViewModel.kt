package com.mulkora.feature.contracts.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.contracts.domain.model.Contract
import com.mulkora.feature.contracts.domain.repository.ContractRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ContractListScreen için ViewModel.
 *
 * Kullanıcının dahil olduğu tüm sözleşmeleri gerçek zamanlı dinler.
 * CLAUDE.md §33: Compose ekranı ViewModel aracılığıyla veri alır,
 * doğrudan Firestore'a erişmez.
 */
@HiltViewModel
class ContractListViewModel @Inject constructor(
    private val repository: ContractRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContractListUiState())
    val uiState: StateFlow<ContractListUiState> = _uiState.asStateFlow()

    init {
        loadContracts()
    }

    private fun loadContracts() {
        repository.observeUserContracts()
            .onEach { result ->
                _uiState.update { state ->
                    when (result) {
                        is Result.Loading -> state.copy(isLoading = true, errorMessage = null)
                        is Result.Success -> state.copy(
                            isLoading = false,
                            contracts = result.data,
                            errorMessage = null,
                        )
                        is Result.Error -> state.copy(
                            isLoading = false,
                            errorMessage = result.message ?: "Bilinmeyen bir hata oluştu.",
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class ContractListUiState(
    val isLoading: Boolean = true,
    val contracts: List<Contract> = emptyList(),
    val errorMessage: String? = null,
)