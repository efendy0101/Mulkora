package com.mulkora.feature.settings.presentation.datadownload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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
 * KVKK — Kişisel Veri Dışa Aktarma ViewModel.
 *
 * CLAUDE.md §30: KVKK veri talepleri
 * docs/22-development-phases.md — Faz 10
 *
 * Kullanıcının kişisel verilerini dışa aktarma talebini
 * [requestDataExport] Cloud Function aracılığıyla başlatır.
 * Tamamlanınca kullanıcıya e-posta ile bağlantı gönderilir.
 */
@HiltViewModel
class DataDownloadViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataDownloadUiState())
    val uiState: StateFlow<DataDownloadUiState> = _uiState.asStateFlow()

    fun requestDataExport() {
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                functions
                    .getHttpsCallable("requestDataExport")
                    .call(mapOf("uid" to uid))
                    .await()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        requestSent = true,
                        userEmail = email,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Veri talebi gönderilemedi. Lütfen tekrar deneyin.",
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class DataDownloadUiState(
    val isLoading: Boolean = false,
    val requestSent: Boolean = false,
    /** Talep başarılıysa kullanıcıya gösterilecek e-posta adresi. */
    val userEmail: String = "",
    val error: String? = null,
)