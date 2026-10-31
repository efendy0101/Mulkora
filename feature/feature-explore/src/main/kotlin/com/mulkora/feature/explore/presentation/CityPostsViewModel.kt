package com.mulkora.feature.explore.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.explore.domain.model.ExplorePost
import com.mulkora.feature.explore.domain.repository.ExploreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CityPostsUiState {
    data object Loading : CityPostsUiState
    data object Empty : CityPostsUiState
    data class Success(val posts: List<ExplorePost>) : CityPostsUiState
    data class Error(val message: String) : CityPostsUiState
}

/**
 * Şehre göre gönderi listesi ViewModel.
 *
 * CLAUDE.md §33: ViewModel; SavedStateHandle'dan şehir alır, repository'den yükler.
 */
@HiltViewModel
class CityPostsViewModel @Inject constructor(
    private val repository: ExploreRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val city: String = checkNotNull(savedStateHandle["city"])

    private val _uiState = MutableStateFlow<CityPostsUiState>(CityPostsUiState.Loading)
    val uiState: StateFlow<CityPostsUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            repository.getPostsByCity(city).collect { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> CityPostsUiState.Loading
                    is Result.Success -> if (result.data.isEmpty())
                        CityPostsUiState.Empty
                    else
                        CityPostsUiState.Success(result.data)
                    is Result.Error -> CityPostsUiState.Error(
                        result.exception?.message ?: "Gönderiler yüklenemedi."
                    )
                }
            }
        }
    }
}