package com.mulkora.feature.contracts.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.contracts.domain.repository.ContractRepository
import com.mulkora.feature.contracts.domain.repository.CreateContractInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sözleşme oluşturma formu için ViewModel.
 *
 * CLAUDE.md §14: Sözleşme oluşturma — taslak kaydetme adımı.
 * Oluşturma işlemi Cloud Function (createContract) üzerinden gerçekleşir;
 * istemci doğrudan Firestore'a yazmaz.
 */
@HiltViewModel
class ContractCreateViewModel @Inject constructor(
    private val repository: ContractRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContractCreateUiState())
    val uiState: StateFlow<ContractCreateUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ContractCreateEvent>()
    val events: SharedFlow<ContractCreateEvent> = _events.asSharedFlow()

    // --- Alan güncellemeleri ---

    fun onPropertyAddressChange(value: String) =
        _uiState.update { it.copy(propertyAddress = value) }

    fun onPropertyTypeChange(value: String) =
        _uiState.update { it.copy(propertyType = value) }

    fun onMonthlyRentChange(value: String) =
        _uiState.update { it.copy(monthlyRentRaw = value) }

    fun onPaymentDayChange(value: String) =
        _uiState.update { it.copy(paymentDayRaw = value) }

    fun onPaymentMethodChange(value: String) =
        _uiState.update { it.copy(paymentMethod = value) }

    fun onDepositAmountChange(value: String) =
        _uiState.update { it.copy(depositAmountRaw = value) }

    fun onMonthlyFeeChange(value: String) =
        _uiState.update { it.copy(monthlyFeeRaw = value) }

    fun onStartDateChange(value: String) =
        _uiState.update { it.copy(startDate = value) }

    fun onEndDateChange(value: String) =
        _uiState.update { it.copy(endDate = value) }

    fun onSpecialConditionsChange(value: String) =
        _uiState.update { it.copy(specialConditions = value) }

    fun onInventoryItemAdd(item: String) {
        if (item.isBlank()) return
        _uiState.update { it.copy(inventoryItems = it.inventoryItems + item.trim()) }
    }

    fun onInventoryItemRemove(item: String) {
        _uiState.update { it.copy(inventoryItems = it.inventoryItems - item) }
    }

    // --- Formu gönder ---

    fun submit() {
        val state = _uiState.value
        val validationError = state.validate()
        if (validationError != null) {
            _uiState.update { it.copy(fieldError = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, fieldError = null) }

            val input = CreateContractInput(
                propertyAddress = state.propertyAddress.trim(),
                propertyType = state.propertyType.trim(),
                monthlyRent = state.monthlyRentRaw.toDoubleOrNull() ?: 0.0,
                paymentDay = state.paymentDayRaw.toIntOrNull() ?: 1,
                paymentMethod = state.paymentMethod.trim(),
                depositAmount = state.depositAmountRaw.toDoubleOrNull() ?: 0.0,
                monthlyFee = state.monthlyFeeRaw.takeIf { it.isNotBlank() }?.toDoubleOrNull(),
                startDate = state.startDate.trim(),
                endDate = state.endDate.trim(),
                specialConditions = state.specialConditions.takeIf { it.isNotBlank() },
                inventoryItems = state.inventoryItems,
            )

            when (val result = repository.createContract(input)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(ContractCreateEvent.ContractCreated(result.data))
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(
                        ContractCreateEvent.ShowError(
                            result.message ?: "Sözleşme oluşturulamadı."
                        )
                    )
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearFieldError() {
        _uiState.update { it.copy(fieldError = null) }
    }
}

data class ContractCreateUiState(
    val propertyAddress: String = "",
    val propertyType: String = "",
    val monthlyRentRaw: String = "",
    val paymentDayRaw: String = "1",
    val paymentMethod: String = "",
    val depositAmountRaw: String = "",
    val monthlyFeeRaw: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val specialConditions: String = "",
    val inventoryItems: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val fieldError: String? = null,
) {
    fun validate(): String? {
        if (propertyAddress.isBlank()) return "Taşınmaz adresi zorunludur."
        if (propertyType.isBlank()) return "Taşınmaz türü zorunludur."
        if (monthlyRentRaw.toDoubleOrNull() == null || monthlyRentRaw.toDouble() <= 0)
            return "Geçerli bir aylık kira miktarı girin."
        val day = paymentDayRaw.toIntOrNull()
        if (day == null || day < 1 || day > 31) return "Ödeme günü 1-31 arasında olmalıdır."
        if (paymentMethod.isBlank()) return "Ödeme yöntemi zorunludur."
        if (depositAmountRaw.toDoubleOrNull() == null) return "Geçerli bir depozito miktarı girin."
        if (startDate.isBlank()) return "Kira başlangıç tarihi zorunludur."
        if (endDate.isBlank()) return "Kira bitiş tarihi zorunludur."
        return null
    }
}

sealed class ContractCreateEvent {
    data class ContractCreated(val contractId: String) : ContractCreateEvent()
    data class ShowError(val message: String) : ContractCreateEvent()
}