package com.mulkora.feature.profile.presentation.blockedusers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.profile.domain.model.BlockedUser
import com.mulkora.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedUsersUiState(
    val blockedUsers: List<BlockedUser> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Engellenen kullanıcılar listesi ViewModel.
 * CLAUDE.md §8: Kullanıcı engelleme işlemi blockUser Cloud Function üzerinden yapılır.
 */
@HiltViewModel
class BlockedUsersViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockedUsersUiState())
    val uiState: StateFlow<BlockedUsersUiState> = _uiState.asStateFlow()

    init {
        loadBlockedUsers()
    }

    private fun loadBlockedUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = profileRepository.getBlockedUsers()) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, blockedUsers = result.data, errorMessage = null)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message ?: "Bir hata oluştu.")
                }
                is Result.Loading -> Unit // Zaten set edildi
            }
        }
    }

    fun unblockUser(targetUid: String) {
        viewModelScope.launch {
            val result = profileRepository.unblockUser(targetUid)
            if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.message ?: "Engel kaldırılamadı.") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}