package com.mulkora.feature.reports.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import com.mulkora.core.common.model.ReportReason
import com.mulkora.feature.reports.domain.model.ReportTargetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Şikayet oluşturma ViewModel.
 * CLAUDE.md Bölüm 11: Şikâyet Sistemi.
 *
 * Kurallar:
 * - Aynı kullanıcı aynı içeriği yalnızca bir kez şikâyet edebilir (backend kontrolü).
 * - Şikayet Cloud Function (createReport) üzerinden gönderilir.
 * - Doğrudan Firestore'a şikayet kaydı yazılamaz (firestore.rules: allow create: if false).
 *
 * targetId ve targetType NavGraphs.kt tarafından doğrudan Composable'a geçirilir,
 * SavedStateHandle üzerinden değil.
 */
data class ReportUiState(
    val selectedReason: ReportReason? = null,
    val additionalNote: String = "",
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val functions: FirebaseFunctions,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun selectReason(reason: ReportReason) {
        _uiState.update { it.copy(selectedReason = reason) }
    }

    fun onAdditionalNoteChanged(note: String) {
        _uiState.update { it.copy(additionalNote = note) }
    }

    fun submitReport(targetId: String, targetType: ReportTargetType) {
        val state = _uiState.value
        val reason = state.selectedReason ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val data = hashMapOf(
                    "contentType"    to targetType.value,
                    "contentId"      to targetId,
                    "reason"         to reason.name,
                    "additionalNote" to state.additionalNote.trim(),
                )
                functions
                    .getHttpsCallable("createReport")
                    .call(data)
                    .await()
                _uiState.update { it.copy(isSubmitting = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Şikayet gönderilemedi.",
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}