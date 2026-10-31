package com.mulkora.feature.postdetail.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.feature.postdetail.domain.model.PostDetail
import com.mulkora.feature.postdetail.domain.repository.PostDetailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Gönderi detay ekranı ViewModel'i.
 *
 * CLAUDE.md §33: ViewModel; UI state'ini tutar, repository'den veri alır.
 * Ekranlar doğrudan Firestore'a erişemez.
 */
sealed interface PostDetailUiState {
    data object Loading : PostDetailUiState
    data class Success(val post: PostDetail) : PostDetailUiState
    data class Error(val message: String) : PostDetailUiState
}

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val repository: PostDetailRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle["postId"])

    private val _uiState = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Loading)
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val _saveResult = MutableStateFlow<String?>(null)
    val saveResult: StateFlow<String?> = _saveResult.asStateFlow()

    init {
        observePost()
    }

    private fun observePost() {
        viewModelScope.launch {
            repository.observePostDetail(postId).collect { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> PostDetailUiState.Loading
                    is Result.Success -> PostDetailUiState.Success(result.data)
                    is Result.Error -> PostDetailUiState.Error(
                        result.exception?.message ?: "Gönderi yüklenemedi."
                    )
                }
            }
        }
    }

    fun toggleSavePost() {
        viewModelScope.launch {
            when (repository.toggleSavePost(postId)) {
                is Result.Success -> _saveResult.value = "Gönderi kaydedildi."
                is Result.Error -> _saveResult.value = "İşlem başarısız oldu."
                else -> Unit
            }
        }
    }

    /**
     * Beğeni (vote=1) veya eksi oy (vote=-1) ekler/kaldırır.
     * CLAUDE.md §9: Sayaç atomik olarak güncellenir.
     */
    fun toggleVote(vote: Int) {
        viewModelScope.launch {
            when (repository.toggleVote(postId, vote)) {
                is Result.Error -> _saveResult.value = "Tepki kaydedilemedi."
                else -> Unit
            }
        }
    }

    /**
     * Gönderiye 1-5 yıldız puanı verir.
     * CLAUDE.md §9: Kullanıcı başına bir puan.
     */
    fun ratePost(rating: Int) {
        viewModelScope.launch {
            when (repository.ratePost(postId, rating)) {
                is Result.Success -> _saveResult.value = "Puanınız kaydedildi."
                is Result.Error -> _saveResult.value = "Puan kaydedilemedi."
                else -> Unit
            }
        }
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }
}
