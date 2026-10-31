package com.mulkora.feature.explore.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.result.Result
import com.mulkora.feature.explore.domain.model.ExplorePost
import com.mulkora.feature.explore.domain.repository.CategorySection
import com.mulkora.feature.explore.domain.repository.ExploreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keşfet modülü ViewModel'i.
 *
 * SCR-040 Keşfet ana ekranı: Kategori bölümlerini gerçek zamanlı dinler.
 * SCR-041 Kategori gönderileri: Belirli bir kategoriye göre filtreler.
 * SCR-042 Şehir bazlı gönderiler: Şehre göre filtreler.
 *
 * CLAUDE.md §33: UI durumu StateFlow ile yönetilir; Compose ekranları
 * doğrudan Firestore'a erişemez.
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val exploreRepository: ExploreRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        loadFeaturedCategories()
        loadPopularCities()
    }

    private fun loadFeaturedCategories() {
        exploreRepository.observeFeaturedCategories()
            .onEach { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoadingCategories = true) }
                    is Result.Success -> _uiState.update {
                        it.copy(
                            isLoadingCategories = false,
                            categorySections = result.data,
                            errorMessage = null,
                        )
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(
                            isLoadingCategories = false,
                            errorMessage = result.exception.message,
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadPopularCities() {
        exploreRepository.observePopularCities()
            .onEach { result ->
                if (result is Result.Success) {
                    _uiState.update { it.copy(popularCities = result.data) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadPostsByCategory(category: PostCategory) {
        exploreRepository.getPostsByCategory(category)
            .onEach { result ->
                when (result) {
                    is Result.Success -> _uiState.update {
                        it.copy(
                            isLoadingCategoryPosts = false,
                            categoryPosts = result.data,
                            errorMessage = null,
                        )
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(
                            isLoadingCategoryPosts = false,
                            errorMessage = result.exception.message,
                        )
                    }
                    is Result.Loading -> _uiState.update {
                        it.copy(isLoadingCategoryPosts = true, selectedCategory = category)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadPostsByCity(city: String) {
        exploreRepository.getPostsByCity(city)
            .onEach { result ->
                when (result) {
                    is Result.Success -> _uiState.update {
                        it.copy(
                            isLoadingCityPosts = false,
                            cityPosts = result.data,
                            errorMessage = null,
                        )
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(
                            isLoadingCityPosts = false,
                            errorMessage = result.exception.message,
                        )
                    }
                    is Result.Loading -> _uiState.update {
                        it.copy(isLoadingCityPosts = true, selectedCity = city)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class ExploreUiState(
    val isLoadingCategories: Boolean = false,
    val isLoadingCategoryPosts: Boolean = false,
    val isLoadingCityPosts: Boolean = false,
    val categorySections: List<CategorySection> = emptyList(),
    val popularCities: List<String> = emptyList(),
    val selectedCategory: PostCategory? = null,
    val categoryPosts: List<ExplorePost> = emptyList(),
    val selectedCity: String? = null,
    val cityPosts: List<ExplorePost> = emptyList(),
    val errorMessage: String? = null,
)