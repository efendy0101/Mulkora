package com.mulkora.feature.feed.presentation.postedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Gönderi düzenleme ekranı ViewModel'i.
 *
 * docs/17-post-moderation-state-machine.md:
 *   edit_requested → pending_approval geçişi updatePost CF ile yapılır.
 * CLAUDE.md §6: Gönderi yalnızca edit_requested durumundayken düzenlenebilir.
 */
data class PostEditUiState(
    val postId: String = "",
    val title: String = "",
    val content: String = "",
    val moderatorNote: String = "",
    val isLoading: Boolean = false,
    val submitted: Boolean = false,
    val errorMessage: String? = null,
) {
    val isFormValid: Boolean
        get() = title.trim().length in 3..150 &&
                content.trim().length in 10..5000
}

@HiltViewModel
class PostEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val functions: FirebaseFunctions,
) : ViewModel() {

    // NavGraphs'ta "postId" arg adıyla geçirilir (AppRoute.PostEdit.ARG_POST_ID)
    private val postId: String = checkNotNull(savedStateHandle["postId"])

    private val _uiState = MutableStateFlow(PostEditUiState(postId = postId))
    val uiState: StateFlow<PostEditUiState> = _uiState.asStateFlow()

    init {
        loadPost()
    }

    private fun loadPost() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val result = functions
                    .getHttpsCallable("getPostForEdit")
                    .call(mapOf("postId" to postId))
                    .await()

                @Suppress("UNCHECKED_CAST")
                val data = result.data as? Map<String, Any> ?: emptyMap()
                _uiState.update { state ->
                    state.copy(
                        title = data["title"] as? String ?: "",
                        content = data["content"] as? String ?: "",
                        moderatorNote = data["moderatorNote"] as? String ?: "",
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                // Yükleme başarısız: mevcut boş formla devam et
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Gönderi yüklenemedi: ${e.localizedMessage}",
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun onContentChange(value: String) {
        _uiState.update { it.copy(content = value) }
    }

    fun onSubmitEdit() {
        val state = _uiState.value
        if (!state.isFormValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                functions
                    .getHttpsCallable("updatePost")
                    .call(
                        mapOf(
                            "postId" to state.postId,
                            "title" to state.title.trim(),
                            "content" to state.content.trim(),
                        )
                    )
                    .await()

                _uiState.update { it.copy(isLoading = false, submitted = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Düzenleme gönderilemedi: ${e.localizedMessage}",
                    )
                }
            }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}