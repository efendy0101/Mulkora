package com.mulkora.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.auth.domain.repository.AuthRepository
import com.mulkora.feature.billing.domain.repository.BillingRepository
import com.mulkora.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Ayarlar ekranı ViewModel.
 *
 * CLAUDE.md §20: Tüm ayar bölümlerini (Hesap, Gizlilik, Reklam & Satın Alma,
 * Bilgilendirme, Hesap İşlemleri) yönetir.
 * CLAUDE.md §18: "Satın Almaları Geri Yükle" Google Play Billing üzerinden yapılır.
 * CLAUDE.md §27: Veri indirme talebi Cloud Functions üzerinden güvenli şekilde işlenir.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val billingRepository: BillingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // ── Hesap ──────────────────────────────────────────────────────────────

    fun onSignOutClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = authRepository.signOut()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            navigationEvent = SettingsNavigationEvent.NavigateToLogin,
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = result.message ?: "Çıkış yapılamadı",
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun onDeleteAccountClick() {
        _uiState.update { it.copy(showDeleteAccountDialog = true) }
    }

    fun onDeleteAccountConfirmed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showDeleteAccountDialog = false) }
            when (val result = authRepository.deleteAccount()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            navigationEvent = SettingsNavigationEvent.NavigateToLogin,
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = result.message ?: "Hesap silinemedi",
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun onDeleteAccountDismissed() {
        _uiState.update { it.copy(showDeleteAccountDialog = false) }
    }

    fun onSendEmailVerificationClick() {
        viewModelScope.launch {
            when (val result = authRepository.sendEmailVerification()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(snackbarMessage = "Doğrulama e-postası gönderildi")
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(snackbarMessage = result.message ?: "E-posta gönderilemedi")
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    // ── Gizlilik ───────────────────────────────────────────────────────────

    /**
     * CLAUDE.md §27 / docs/22-development-phases.md:
     * Kullanıcı verilerini indirme talebi Cloud Functions üzerinden (requestDataExport)
     * güvenli biçimde oluşturulur. Hazırlandığında kullanıcıya bildirim gelir.
     */
    fun onRequestDataExportClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = profileRepository.requestDataExport()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = "Veri indirme talebiniz alındı. Hazır olduğunda bildirim alacaksınız.",
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = result.message ?: "Talep gönderilemedi",
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    // ── Reklam ve Satın Alma ───────────────────────────────────────────────

    /**
     * CLAUDE.md §18: Satın alma geri yükleme işlemi BillingRepository üzerinden
     * Google Play Billing API'ya iletilir. Yerel depolamaya güvenilmez; her zaman
     * backend doğrulaması yapılır.
     */
    fun onRestorePurchasesClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = billingRepository.restorePurchases()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = "Satın almalar başarıyla geri yüklendi.",
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = result.message ?: "Geri yükleme başarısız",
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    // ── Yardımcı ───────────────────────────────────────────────────────────

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigationEvent = null) }
    }
}

data class SettingsUiState(
    val isLoading: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val snackbarMessage: String? = null,
    val navigationEvent: SettingsNavigationEvent? = null,
)

sealed interface SettingsNavigationEvent {
    data object NavigateToLogin : SettingsNavigationEvent
}