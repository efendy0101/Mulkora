package com.mulkora.feature.billing.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.feature.billing.domain.model.PurchaseStatus
import com.mulkora.feature.billing.domain.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SCR-080 (RemoveAds) & SCR-081 (PurchaseConfirmation) için ViewModel.
 *
 * CLAUDE.md §33: Compose ekranları doğrudan Billing API'ya erişemez;
 * tüm satın alma işlemleri bu ViewModel üzerinden yönetilir.
 */
@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    init {
        observePurchaseStatus()
    }

    private fun observePurchaseStatus() {
        viewModelScope.launch {
            billingRepository.observePurchaseStatus().collect { status ->
                _uiState.update { it.copy(purchaseStatus = status) }
            }
        }
    }

    fun purchaseRemoveAds() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = billingRepository.launchRemoveAdsPurchase()
            _uiState.update { current ->
                when (result) {
                    is com.mulkora.core.common.result.Result.Success ->
                        current.copy(isLoading = false)
                    is com.mulkora.core.common.result.Result.Error ->
                        current.copy(
                            isLoading = false,
                            errorMessage = result.exception.localizedMessage
                                ?: "Satın alma başlatılamadı",
                        )
                    com.mulkora.core.common.result.Result.Loading ->
                        current.copy(isLoading = true)
                }
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = billingRepository.restorePurchases()
            _uiState.update { current ->
                when (result) {
                    is com.mulkora.core.common.result.Result.Success ->
                        current.copy(isLoading = false)
                    is com.mulkora.core.common.result.Result.Error ->
                        current.copy(
                            isLoading = false,
                            errorMessage = result.exception.localizedMessage
                                ?: "Satın almalar geri yüklenemedi",
                        )
                    com.mulkora.core.common.result.Result.Loading ->
                        current.copy(isLoading = true)
                }
            }
        }
    }

    /**
     * Activity referansını repository'ye iletir.
     * RemoveAdsScreen'de DisposableEffect ile çağrılır.
     */
    fun setActivity(activity: Activity?) {
        billingRepository.setActivity(activity)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        billingRepository.disconnectBilling()
    }
}

/**
 * Faturalandırma ekranı UI durumu.
 */
data class BillingUiState(
    val purchaseStatus: PurchaseStatus = PurchaseStatus.UNKNOWN,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Kullanıcı reklamları kaldırmış mı? */
    val isAdFree: Boolean get() = purchaseStatus == PurchaseStatus.PURCHASED
}