package com.mulkora.feature.comments.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.comments.domain.model.Comment
import com.mulkora.feature.comments.domain.repository.CommentsRepository
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
 * Yorumlar ekranı ViewModel.
 * CLAUDE.md §9: Yorum ve Cevap Sistemi.
 *
 * postId navigation argument'tan SavedStateHandle aracılığıyla okunur.
 * Yorumlar gerçek zamanlı Firestore snapshot ile güncellenir.
 */
@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val commentsRepository: CommentsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle["postId"]) {
        "postId navigation argument eksik"
    }

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()

    init {
        observeComments()
    }

    // ─── Yorumları gerçek zamanlı dinle ──────────────────────────────────────

    private fun observeComments() {
        commentsRepository.observeComments(postId)
            .onEach { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            comments  = result.data,
                            error     = null,
                        )
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            error     = result.message ?: "Yorumlar yüklenemedi.",
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    // ─── Yorum metin değişikliği ──────────────────────────────────────────────

    fun onCommentTextChanged(text: String) {
        _uiState.update { it.copy(newCommentText = text) }
    }

    // ─── Yorum gönder ────────────────────────────────────────────────────────

    fun submitComment() {
        val text = _uiState.value.newCommentText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val result = commentsRepository.addComment(postId = postId, body = text)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isSubmitting = false, newCommentText = "")
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting    = false,
                            feedbackMessage = result.message ?: "Yorum gönderilemedi.",
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    // ─── Yorum beğen ─────────────────────────────────────────────────────────

    fun toggleCommentLike(commentId: String) {
        viewModelScope.launch {
            when (val result = commentsRepository.toggleCommentLike(postId, commentId)) {
                is Result.Error -> _uiState.update {
                    it.copy(feedbackMessage = result.message ?: "Beğeni işlemi başarısız.")
                }
                else -> Unit
            }
        }
    }

    // ─── Yorum sil ───────────────────────────────────────────────────────────

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            when (val result = commentsRepository.deleteComment(postId, commentId)) {
                is Result.Error -> _uiState.update {
                    it.copy(feedbackMessage = result.message ?: "Yorum silinemedi.")
                }
                else -> Unit
            }
        }
    }

    // ─── Geri bildirim mesajını temizle ──────────────────────────────────────

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}

/**
 * Yorumlar ekranı UI durumu.
 */
data class CommentsUiState(
    val isLoading: Boolean        = true,
    val isSubmitting: Boolean     = false,
    val comments: List<Comment>   = emptyList(),
    val newCommentText: String    = "",
    val error: String?            = null,
    val feedbackMessage: String?  = null,
)