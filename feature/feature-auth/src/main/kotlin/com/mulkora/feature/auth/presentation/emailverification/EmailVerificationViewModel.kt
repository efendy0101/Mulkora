package com.mulkora.feature.auth.presentation.emailverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * E-posta doğrulama ekranı ViewModel.
 * CLAUDE.md §2: Kayıt sonrası e-posta doğrulama zorunludur.
 * - Yeniden gönderme: 60 sn cooldown
 * - Token yenile + kontrol: reloadUser() ile Firebase token'ı senkronize edilir.
 */
@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailVerificationUiState())
    val uiState: StateFlow<EmailVerificationUiState> = _uiState.asStateFlow()

    companion object {
        private const val RESEND_COOLDOWN_SECONDS = 60
    }

    init {
        startCooldown()
    }

    private fun startCooldown() {
        viewModelScope.launch {
            _uiState.update { it.copy(resendCooldown = RESEND_COOLDOWN_SECONDS) }
            for (i in RESEND_COOLDOWN_SECONDS downTo 1) {
                _uiState.update { it.copy(resendCooldown = i) }
                delay(1_000)
            }
            _uiState.update { it.copy(resendCooldown = 0) }
        }
    }

    fun onResendClicked() {
        if (_uiState.value.resendCooldown > 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, message = null) }
            when (val result = authRepository.sendEmailVerification()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSending = false, message = "Doğrulama e-postası yeniden gönderildi.") }
                    startCooldown()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isSending = false, message = result.message ?: "E-posta gönderilemedi.")
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Firebase token'ı yenile, e-posta doğrulandıysa ana ekrana geç.
     */
    fun onCheckVerification(onVerified: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, message = null) }
            when (val result = authRepository.reloadUser()) {
                is Result.Success -> {
                    if (result.data.isEmailVerified) {
                        _uiState.update { it.copy(isChecking = false) }
                        onVerified()
                    } else {
                        _uiState.update {
                            it.copy(isChecking = false, message = "E-posta henüz doğrulanmamış. Lütfen gelen kutunuzu kontrol edin.")
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isChecking = false, message = result.message ?: "Kontrol edilemedi.")
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun onSignOutClicked(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onSignedOut()
        }
    }
}

data class EmailVerificationUiState(
    val isSending: Boolean = false,
    val isChecking: Boolean = false,
    val resendCooldown: Int = 0,
    val message: String? = null
)