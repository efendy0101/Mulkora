package com.mulkora.feature.reactions.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.feature.reactions.domain.model.PostReaction
import com.mulkora.feature.reactions.domain.model.ReactionType
import com.mulkora.feature.reactions.domain.repository.ReactionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Beğeni, eksi oy ve puanlama ViewModel.
 *
 * CLAUDE.md §10: Sayaçlar doğrudan istemci tarafından değiştirilemez.
 * Tüm yazma işlemleri Cloud Functions üzerinden gerçekleştirilir.
 *
 * [postId] SavedStateHandle'dan alınır; bu ViewModel tipik olarak
 * PostDetailScreen veya FeedScreen'in alt composable'larından kullanılır.
 */
@HiltViewModel
class ReactionsViewModel @Inject constructor(
    private val reactionsRepository: ReactionsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle["postId"])

    private val _uiState = MutableStateFlow(ReactionsUiState())
    val uiState: StateFlow<ReactionsUiState> = _uiState.asStateFlow()

    init {
        observeReactions()
    }

    private fun observeReactions() {
        reactionsRepository.observePostReaction(postId)
            .onEach { reaction ->
                _uiState.value = _uiState.value.copy(
                    reaction = reaction,
                    isLoading = false,
                    error = null,
                )
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * Beğeni veya eksi oy gönderir.
     * Aynı reaksiyon zaten varsa toggle ile kaldırılır (bu mantık Cloud Function'da).
     */
    fun react(type: ReactionType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true)
            reactionsRepository.reactToPost(postId, type)
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        error = e.message,
                    )
                }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isActionLoading = false)
                }
        }
    }

    /**
     * 1–5 yıldız puan gönderir.
     *
     * @param rating 1–5 arası puan değeri
     */
    fun rate(rating: Int) {
        if (rating !in 1..5) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true)
            reactionsRepository.ratePost(postId, rating)
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        error = e.message,
                    )
                }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isActionLoading = false)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ReactionsUiState(
    val reaction: PostReaction? = null,
    val isLoading: Boolean = true,
    val isActionLoading: Boolean = false,
    val error: String? = null,
)