package com.mulkora.feature.explore.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.result.Result
import com.mulkora.feature.explore.domain.model.ExplorePost
import com.mulkora.feature.explore.domain.repository.ExploreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CategoryPostsUiState {
    data object Loading : CategoryPostsUiState
    data object Empty : CategoryPostsUiState
    data class Success(val posts: List<ExplorePost>) : CategoryPostsUiState
    data class Error(val message: String) : CategoryPostsUiState
}

/**
 * Kategori gönderileri ViewModel.
 *
 * CLAUDE.md §33: ViewModel; SavedStateHandle'dan kategori alır, repository'den yükler.
 */
@HiltViewModel
class CategoryPostsViewModel @Inject constructor(
    private val repository: ExploreRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val categoryName: String = checkNotNull(savedStateHandle["category"])
    private val category: PostCategory = PostCategory.fromValue(
        categoryName.lowercase().replace("_", "_")
    ).let {
        // SavedStateHandle'dan enum name veya value gelebilir
        runCatching { PostCategory.valueOf(categoryName) }.getOrElse {
            PostCategory.fromValue(categoryName)
        }
    }

    private val _uiState = MutableStateFlow<CategoryPostsUiState>(CategoryPostsUiState.Loading)
    val uiState: StateFlow<CategoryPostsUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            repository.getPostsByCategory(category).collect { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> CategoryPostsUiState.Loading
                    is Result.Success -> if (result.data.isEmpty())
                        CategoryPostsUiState.Empty
                    else
                        CategoryPostsUiState.Success(result.data)
                    is Result.Error -> CategoryPostsUiState.Error(
                        result.exception?.message ?: "Gönderiler yüklenemedi."
                    )
                }
            }
        }
    }
}