package com.mulkora.feature.postcreate.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.model.PostType
import com.mulkora.core.common.result.Result
import com.mulkora.core.firebase.storage.ImageUploadRepository
import com.mulkora.feature.feed.domain.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Gönderi oluşturma ViewModel.
 * CLAUDE.md Bölüm 7: Gönderi Oluşturma
 * CLAUDE.md Bölüm 8: Gönderi Onay Sistemi
 *
 * Akış:
 * 1. Kullanıcı formu doldurur.
 * 2. Görsel seçilirse EXIF temizlenir (ImageUploadRepository).
 * 3. Submit: görsel varsa önce Storage'a yüklenir, URL alınır.
 * 4. Gönderi Firestore'da draft olarak oluşturulur.
 * 5. submitPost CF çağrılır: draft → pending (moderasyon kuyruğu).
 *
 * Doğrulama kuralları:
 * - Başlık: en az 10, en fazla 200 karakter
 * - İçerik: en az 20, en fazla 5000 karakter
 * - En fazla 5 etiket
 * - Görsel: isteğe bağlı, tek görsel (CLAUDE.md §15)
 */
@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val imageUploadRepository: ImageUploadRepository,
) : ViewModel() {

    companion object {
        const val TITLE_MIN_LENGTH   = 10
        const val TITLE_MAX_LENGTH   = 200
        const val CONTENT_MIN_LENGTH = 20
        const val CONTENT_MAX_LENGTH = 5000
        const val MAX_TAGS           = 5
        const val TAG_MAX_LENGTH     = 30
    }

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun onTitleChange(title: String) {
        if (title.length <= TITLE_MAX_LENGTH) {
            _uiState.update { it.copy(title = title, titleError = null) }
        }
    }

    fun onContentChange(content: String) {
        if (content.length <= CONTENT_MAX_LENGTH) {
            _uiState.update { it.copy(content = content, contentError = null) }
        }
    }

    fun onPostTypeSelected(postType: PostType) {
        _uiState.update { it.copy(selectedPostType = postType) }
    }

    fun onCategorySelected(category: PostCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onCityChanged(city: String) {
        _uiState.update { it.copy(city = city) }
    }

    fun onAnonymousToggled(anonymous: Boolean) {
        _uiState.update { it.copy(isAnonymous = anonymous) }
    }

    fun onAllowCommentsToggled(allow: Boolean) {
        _uiState.update { it.copy(allowComments = allow) }
    }

    fun onTagAdded(tag: String) {
        val cleanTag = tag.trim().lowercase()
        if (cleanTag.isBlank()) return
        if (cleanTag.length > TAG_MAX_LENGTH) return
        val state = _uiState.value
        if (state.tags.size >= MAX_TAGS) {
            _uiState.update { it.copy(tagsError = "En fazla $MAX_TAGS etiket eklenebilir") }
            return
        }
        if (state.tags.contains(cleanTag)) return
        _uiState.update {
            it.copy(tags = it.tags + cleanTag, tagsError = null)
        }
    }

    fun onTagRemoved(tag: String) {
        _uiState.update { it.copy(tags = it.tags - tag) }
    }

    /**
     * Görsel seçildiğinde URI'ı state'e kaydeder.
     * EXIF temizleme ve Storage yüklemesi submit anında yapılır.
     *
     * @param uri Galeri/kamera URI
     */
    fun onImageSelected(uri: Uri?) {
        _uiState.update {
            it.copy(
                selectedImageUri = uri,
                pendingImageStorageUrl = null, // Yeni görsel seçildi; önceki URL'yi sıfırla
                generalError = null,
            )
        }
    }

    /**
     * Screen'den Context ile gelen çağrı.
     * CLAUDE.md §16: EXIF temizleme aslında Storage'a yükleme anında yapılır (ImageUploadRepository).
     */
    fun onImageSelectedWithContext(context: Context, uri: Uri) {
        onImageSelected(uri)
    }

    fun onImageRemoved() {
        _uiState.update {
            it.copy(
                selectedImageUri = null,
                pendingImageStorageUrl = null,
            )
        }
    }

    /** Ekrandan onSubmitPost() çağrısı ile uyumlu takma ad */
    fun onSubmitPost() = onSubmitClick()

    fun onSubmitClick() {
        val state = _uiState.value
        if (!validate(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            // 1. Görsel varsa önce Firebase Storage'a yükle
            var storageImageUrl: String? = null
            val selectedUri = state.selectedImageUri
            if (selectedUri != null) {
                _uiState.update { it.copy(isUploadingImage = true) }
                when (val uploadResult = imageUploadRepository.uploadPostImage(selectedUri)) {
                    is Result.Success -> {
                        storageImageUrl = uploadResult.data
                        _uiState.update { it.copy(isUploadingImage = false) }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isUploadingImage = false,
                                generalError = uploadResult.message
                                    ?: "Görsel yüklenemedi. Lütfen tekrar deneyin.",
                            )
                        }
                        return@launch
                    }
                    is Result.Loading -> Unit
                }
            }

            // 2. Gönderiyi oluştur (FeedRepositoryImpl içinde submitPost CF'yi de çağırır)
            when (val result = feedRepository.createPost(
                title = state.title.trim(),
                content = state.content.trim(),
                postType = state.selectedPostType,
                category = state.selectedCategory,
                tags = state.tags,
                imageUri = storageImageUrl,
            )) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            navigationEvent = CreatePostNavigationEvent.PostCreated(result.data),
                        )
                    }
                }
                is Result.Error -> {
                    // Görsel yüklendi ama gönderi oluşturulamadı.
                    //
                    // Görseli SILME: FeedRepositoryImpl.createPost() önce Firestore'a
                    // taslağı yazar, ardından submitPost CF'yi çağırır. CF hatası durumunda
                    // Firestore taslağı zaten oluşmuş olabilir ve imageUrl'i içerir.
                    // Görseli silersek taslak belgesi bozuk referansla kalır.
                    //
                    // Beklenen davranış: Kullanıcıya hata göster, taslak Firestore'da korunur,
                    // kullanıcı uygulamayı yeniden açtığında taslağı görebilir veya tekrar gönderebilir.
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = result.message
                                ?: "Gönderi moderasyon kuyruğuna gönderilemedi. Taslağınız kaydedildi, daha sonra tekrar deneyebilirsiniz.",
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigationEvent = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(generalError = null) }
    }

    private fun validate(state: CreatePostUiState): Boolean {
        var isValid = true
        var newState = state

        if (state.title.trim().length < TITLE_MIN_LENGTH) {
            newState = newState.copy(
                titleError = "Başlık en az $TITLE_MIN_LENGTH karakter olmalıdır"
            )
            isValid = false
        }

        if (state.content.trim().length < CONTENT_MIN_LENGTH) {
            newState = newState.copy(
                contentError = "İçerik en az $CONTENT_MIN_LENGTH karakter olmalıdır"
            )
            isValid = false
        }

        _uiState.update { newState }
        return isValid
    }
}

data class CreatePostUiState(
    val title: String = "",
    val content: String = "",
    val selectedPostType: PostType = PostType.QUESTION,
    val selectedCategory: PostCategory = PostCategory.GENERAL,
    val tags: List<String> = emptyList(),
    val city: String = "",
    val isAnonymous: Boolean = false,
    val allowComments: Boolean = true,
    /** Kullanıcının seçtiği yerel görsel URI — Storage'a henüz yüklenmedi */
    val selectedImageUri: Uri? = null,
    /** Storage yükleme başarılı olunca set edilir (submit içinde kullanılır) */
    val pendingImageStorageUrl: String? = null,
    val titleError: String? = null,
    val contentError: String? = null,
    val tagsError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    /** Görsel işlenirken veya EXIF temizlenirken true */
    val isProcessingImage: Boolean = false,
    /** Görsel Storage'a yüklenirken true */
    val isUploadingImage: Boolean = false,
    val navigationEvent: CreatePostNavigationEvent? = null,
)

sealed interface CreatePostNavigationEvent {
    data class PostCreated(val postId: String) : CreatePostNavigationEvent
}