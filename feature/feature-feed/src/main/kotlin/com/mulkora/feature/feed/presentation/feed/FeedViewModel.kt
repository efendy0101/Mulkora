package com.mulkora.feature.feed.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.model.PostType
import com.mulkora.core.common.result.Result
import com.mulkora.feature.feed.domain.model.FeedItem
import com.mulkora.feature.feed.domain.model.ReactionType
import com.mulkora.feature.feed.domain.repository.FeedRepository
import com.mulkora.feature.feed.domain.repository.FeedSortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Ana feed ekranı ViewModel.
 * CLAUDE.md Bölüm 7: Gönderi listesi, filtreleme ve sıralama.
 */
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(FeedFilterState())
    val filterState: StateFlow<FeedFilterState> = _filterState.asStateFlow()

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    /**
     * Filtre durumuna göre otomatik güncellenen sayfalandırılmış feed akışı.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val feedItems: Flow<PagingData<FeedItem>> = _filterState
        .flatMapLatest { filter ->
            feedRepository.getApprovedPostsPaged(
                category = filter.selectedCategory,
                postType = filter.selectedPostType,
                sortBy = filter.sortOption
            )
        }
        .cachedIn(viewModelScope)

    fun onCategorySelected(category: PostCategory?) {
        _filterState.update { it.copy(selectedCategory = category) }
    }

    fun onPostTypeSelected(postType: PostType?) {
        _filterState.update { it.copy(selectedPostType = postType) }
    }

    fun onSortOptionSelected(sortOption: FeedSortOption) {
        _filterState.update { it.copy(sortOption = sortOption) }
    }

    fun onReactionClick(postId: String, reactionType: ReactionType) {
        viewModelScope.launch {
            when (val result = feedRepository.toggleReaction(postId, reactionType)) {
                is Result.Error -> {
                    _uiState.update {
                        it.copy(snackbarMessage = result.message ?: "İşlem başarısız")
                    }
                }
                else -> Unit
            }
        }
    }

    fun onSavePostClick(postId: String) {
        viewModelScope.launch {
            when (val result = feedRepository.toggleSavePost(postId)) {
                is Result.Error -> {
                    _uiState.update {
                        it.copy(snackbarMessage = result.message ?: "Kaydetme başarısız")
                    }
                }
                else -> Unit
            }
        }
    }

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}

data class FeedFilterState(
    val selectedCategory: PostCategory? = null,
    val selectedPostType: PostType? = null,
    val sortOption: FeedSortOption = FeedSortOption.NEWEST
)

data class FeedUiState(
    val snackbarMessage: String? = null
)