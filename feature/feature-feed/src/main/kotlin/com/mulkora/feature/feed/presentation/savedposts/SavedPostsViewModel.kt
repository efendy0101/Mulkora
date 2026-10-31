package com.mulkora.feature.feed.presentation.savedposts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.feed.domain.model.Post
import com.mulkora.feature.feed.domain.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedPostsUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Kaydedilen gönderiler ViewModel.
 * CLAUDE.md §7: Gönderi kaydetme/kaldırma işlemi FeedRepository üzerinden,
 * sayaç değişikliği Cloud Functions ile yapılır.
 */
@HiltViewModel
class SavedPostsViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedPostsUiState())
    val uiState: StateFlow<SavedPostsUiState> = _uiState.asStateFlow()

    init {
        observeSavedPosts()
    }

    private fun observeSavedPosts() {
        feedRepository.getSavedPosts()
            .onEach { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    is Result.Success -> _uiState.update {
                        it.copy(isLoading = false, posts = result.data, errorMessage = null)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message ?: "Bir hata oluştu.")
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun unsavePost(postId: String) {
        viewModelScope.launch {
            feedRepository.toggleSavePost(postId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}