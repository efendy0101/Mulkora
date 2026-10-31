package com.mulkora.feature.profile.presentation.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.core.common.result.onError
import com.mulkora.feature.profile.domain.model.UserProfile
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

data class ProfileUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isCurrentUser: Boolean = false,
)

/**
 * CLAUDE.md §8: Hem kendi profil hem başkasının profilini destekler.
 * SavedStateHandle üzerinden "userId" argümanı gelirse o kullanıcının profili,
 * gelmezse oturum açık kullanıcının profili yüklenir.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        const val ARG_USER_ID = "userId"
    }

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        val targetUid = savedStateHandle.get<String>(ARG_USER_ID)
        if (targetUid != null) {
            loadUserProfile(targetUid)
        } else {
            loadCurrentUserProfile()
        }
    }

    private fun loadCurrentUserProfile() {
        repository.currentUserProfile
            .onEach { result ->
                when (result) {
                    is Result.Success -> _uiState.update {
                        it.copy(
                            profile = result.data,
                            isLoading = false,
                            isCurrentUser = true,
                        )
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadUserProfile(uid: String) {
        repository.getUserProfile(uid)
            .onEach { result ->
                when (result) {
                    is Result.Success -> _uiState.update {
                        it.copy(
                            profile = result.data,
                            isLoading = false,
                            isCurrentUser = false,
                        )
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun blockUser(targetUid: String) {
        viewModelScope.launch {
            repository.blockUser(targetUid).onError { _, message ->
                _uiState.update { it.copy(errorMessage = message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
