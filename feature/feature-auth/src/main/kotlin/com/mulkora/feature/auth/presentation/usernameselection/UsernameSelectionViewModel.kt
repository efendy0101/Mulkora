package com.mulkora.feature.auth.presentation.usernameselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Kullanıcı adı seçim ekranı ViewModel.
 * CLAUDE.md §4: Kullanıcı adı benzersizliği backend üzerinden kontrol edilir.
 */
@HiltViewModel
class UsernameSelectionViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsernameSelectionUiState())
    val uiState: StateFlow<UsernameSelectionUiState> = _uiState.asStateFlow()

    private val _usernameQuery = MutableStateFlow("")

    init {
        observeUsernameAvailability()
    }

    @OptIn(FlowPreview::class)
    private fun observeUsernameAvailability() {
        _usernameQuery
            .debounce(400)
            .distinctUntilChanged()
            .filter { it.length >= 3 }
            .onEach { username ->
                _uiState.update { it.copy(isChecking = true, availabilityError = null) }
                val available = authRepository.isUsernameAvailable(username)
                _uiState.update { state ->
                    state.copy(
                        isChecking = false,
                        isAvailable = available,
                        availabilityError = if (!available) "Bu kullanıcı adı zaten alınmış." else null
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onUsernameChanged(username: String) {
        val cleaned = username.lowercase().filter { it.isLetterOrDigit() || it == '_' }
        _uiState.update {
            it.copy(
                username = cleaned,
                isAvailable = null,
                availabilityError = if (cleaned.length < 3 && cleaned.isNotEmpty())
                    "Kullanıcı adı en az 3 karakter olmalıdır." else null
            )
        }
        _usernameQuery.value = cleaned
    }

    fun onContinueClicked(onSuccess: () -> Unit) {
        val username = _uiState.value.username
        if (username.length < 3) {
            _uiState.update { it.copy(availabilityError = "Geçerli bir kullanıcı adı girin.") }
            return
        }
        if (_uiState.value.isAvailable != true) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.updateUsername(username)
            _uiState.update { it.copy(isLoading = false) }
            if (result) onSuccess() else {
                _uiState.update { it.copy(availabilityError = "Kullanıcı adı kaydedilemedi. Lütfen tekrar deneyin.") }
            }
        }
    }
}

data class UsernameSelectionUiState(
    val username: String = "",
    val isChecking: Boolean = false,
    val isLoading: Boolean = false,
    val isAvailable: Boolean? = null,
    val availabilityError: String? = null
)