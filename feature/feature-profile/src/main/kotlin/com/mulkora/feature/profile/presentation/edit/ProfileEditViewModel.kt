package com.mulkora.feature.profile.presentation.edit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulkora.core.common.result.Result
import com.mulkora.core.common.util.ExifStripper
import com.mulkora.feature.profile.domain.model.UpdateProfileRequest
import com.mulkora.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Profil düzenleme ViewModel.
 * CLAUDE.md §8: Kullanıcı bio ve konum günceller.
 * - Kullanıcı adı UsernameSelection ekranından değiştirilir (burada değil).
 * - Rol backend/admin tarafından yönetilir; bu ekranda değiştirilemez.
 */
@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val result = profileRepository.currentUserProfile.first()
                if (result is Result.Success) {
                    val profile = result.data
                    if (profile != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                displayName = profile.displayName,
                                bio = profile.bio ?: "",
                                city = profile.city ?: "",
                                photoUrl = profile.photoUrl
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Profil yüklenemedi.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Profil yüklenemedi.") }
            }
        }
    }

    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayName = value, error = null) }
    }

    fun onBioChange(value: String) {
        if (value.length <= BIO_MAX_LENGTH) {
            _uiState.update { it.copy(bio = value) }
        }
    }

    fun onCityChange(value: String) {
        _uiState.update { it.copy(city = value) }
    }

    /**
     * Kullanıcı yeni profil fotoğrafı seçtiğinde çağrılır.
     *
     * CLAUDE.md §16 güvenlik kuralı:
     * - ExifStripper GPS koordinatlarını ve cihaz bilgisini kaldırır
     * - Temizlenmiş ByteArray doğrudan Storage'a yüklenir (URI değil)
     * - Context IO dispatcher'da kullanılır; UI thread bloklanmaz
     *
     * @param context ContentResolver erişimi için gerekli
     * @param imageUri Galeri/kamera'dan gelen görsel URI
     */
    fun onPhotoSelected(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPhoto = true, error = null) }
            try {
                // EXIF temizleme IO thread'inde — bitmap decode bloklamaz UI'ı
                val cleanBytes = withContext(Dispatchers.IO) {
                    ExifStripper.stripAndCompress(context, imageUri)
                }
                when (val result = profileRepository.updateProfilePhotoBytes(cleanBytes)) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(isUploadingPhoto = false, photoUrl = result.data)
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isUploadingPhoto = false,
                                error = result.message ?: "Fotoğraf yüklenemedi.",
                            )
                        }
                    }
                    is Result.Loading -> {}
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isUploadingPhoto = false, error = "Fotoğraf işlenemedi.")
                }
            }
        }
    }

    fun onSaveClicked(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.displayName.isBlank()) {
            _uiState.update { it.copy(error = "Ad Soyad boş olamaz.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val request = UpdateProfileRequest(
                displayName = state.displayName.trim(),
                bio = state.bio.trim(),
                city = state.city.trim()
            )
            when (val result = profileRepository.updateProfile(request)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isSaving = false, error = result.message ?: "Kaydedilemedi.")
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    companion object {
        const val BIO_MAX_LENGTH = 200
    }
}

data class ProfileEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    /** CLAUDE.md §16: Fotoğraf yükleme sırasında ExifStripper çalışıyor. */
    val isUploadingPhoto: Boolean = false,
    val displayName: String = "",
    val bio: String = "",
    val city: String = "",
    val photoUrl: String? = null,
    val error: String? = null,
)
