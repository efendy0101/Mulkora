package com.mulkora.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Giriş ekranı ViewModel.
 * CLAUDE.md: ViewModel sadece UiState yayar; iş mantığı repository'de.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun onSignInClick() {
        val state = _uiState.value
        if (!validate(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            when (val result = authRepository.signInWithEmail(state.email, state.password)) {
                is Result.Success -> {
                    val user = result.data
                    if (!user.isEmailVerified) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                navigationEvent = LoginNavigationEvent.EmailNotVerified
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                navigationEvent = LoginNavigationEvent.NavigateToHome
                            )
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = result.message ?: "Giriş başarısız"
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun onGoogleSignInResult(idToken: String?) {
        if (idToken == null) {
            _uiState.update { it.copy(generalError = "Google girişi başarısız") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            when (val result = authRepository.signInWithGoogle(idToken)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            navigationEvent = LoginNavigationEvent.NavigateToHome
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = result.message ?: "Google girişi başarısız"
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigationEvent = null) }
    }

    private fun validate(state: LoginUiState): Boolean {
        var isValid = true
        var newState = state

        if (state.email.isBlank()) {
            newState = newState.copy(emailError = "E-posta boş olamaz")
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            newState = newState.copy(emailError = "Geçersiz e-posta adresi")
            isValid = false
        }

        if (state.password.isBlank()) {
            newState = newState.copy(passwordError = "Şifre boş olamaz")
            isValid = false
        }

        _uiState.update { newState }
        return isValid
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val navigationEvent: LoginNavigationEvent? = null
)

sealed interface LoginNavigationEvent {
    data object NavigateToHome : LoginNavigationEvent
    data object EmailNotVerified : LoginNavigationEvent
}