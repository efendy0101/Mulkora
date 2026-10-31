package com.mulkora.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Bildirim ayarları ViewModel'i (SCR-134).
 *
 * docs/19-notification-system.md: Bildirim tercihlerini Firestore'a kaydeder.
 * CLAUDE.md §14: Tercihler users/{uid}/notificationPreferences belgesinde tutulur.
 */
data class NotificationSettingsUiState(
    val postStatusEnabled: Boolean = true,
    val newCommentEnabled: Boolean = true,
    val commentReplyEnabled: Boolean = true,
    val referenceInviteEnabled: Boolean = true,
    val referenceStatusEnabled: Boolean = true,
    val referenceResponseEnabled: Boolean = true,
    val contractInviteEnabled: Boolean = true,
    val contractStatusEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    private val uid: String? get() = auth.currentUser?.uid

    private val prefsCollection = "notificationPreferences"

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        val uid = uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val snap = firestore
                    .collection("users")
                    .document(uid)
                    .collection(prefsCollection)
                    .document("preferences")
                    .get()
                    .await()

                if (snap.exists()) {
                    val data = snap.data ?: emptyMap()
                    _uiState.update { state ->
                        state.copy(
                            postStatusEnabled = data["postStatus"] as? Boolean ?: true,
                            newCommentEnabled = data["newComment"] as? Boolean ?: true,
                            commentReplyEnabled = data["commentReply"] as? Boolean ?: true,
                            referenceInviteEnabled = data["referenceInvite"] as? Boolean ?: true,
                            referenceStatusEnabled = data["referenceStatus"] as? Boolean ?: true,
                            referenceResponseEnabled = data["referenceResponse"] as? Boolean ?: true,
                            contractInviteEnabled = data["contractInvite"] as? Boolean ?: true,
                            contractStatusEnabled = data["contractStatus"] as? Boolean ?: true,
                            isLoading = false,
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Tercihler yüklenemedi: ${e.localizedMessage}",
                    )
                }
            }
        }
    }

    private fun savePreferences() {
        val uid = uid ?: return
        val state = _uiState.value
        viewModelScope.launch {
            try {
                firestore
                    .collection("users")
                    .document(uid)
                    .collection(prefsCollection)
                    .document("preferences")
                    .set(
                        mapOf(
                            "postStatus" to state.postStatusEnabled,
                            "newComment" to state.newCommentEnabled,
                            "commentReply" to state.commentReplyEnabled,
                            "referenceInvite" to state.referenceInviteEnabled,
                            "referenceStatus" to state.referenceStatusEnabled,
                            "referenceResponse" to state.referenceResponseEnabled,
                            "contractInvite" to state.contractInviteEnabled,
                            "contractStatus" to state.contractStatusEnabled,
                        )
                    )
                    .await()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Tercihler kaydedilemedi: ${e.localizedMessage}")
                }
            }
        }
    }

    fun onPostStatusToggle(enabled: Boolean) {
        _uiState.update { it.copy(postStatusEnabled = enabled) }
        savePreferences()
    }

    fun onNewCommentToggle(enabled: Boolean) {
        _uiState.update { it.copy(newCommentEnabled = enabled) }
        savePreferences()
    }

    fun onCommentReplyToggle(enabled: Boolean) {
        _uiState.update { it.copy(commentReplyEnabled = enabled) }
        savePreferences()
    }

    fun onReferenceInviteToggle(enabled: Boolean) {
        _uiState.update { it.copy(referenceInviteEnabled = enabled) }
        savePreferences()
    }

    fun onReferenceStatusToggle(enabled: Boolean) {
        _uiState.update { it.copy(referenceStatusEnabled = enabled) }
        savePreferences()
    }

    fun onReferenceResponseToggle(enabled: Boolean) {
        _uiState.update { it.copy(referenceResponseEnabled = enabled) }
        savePreferences()
    }

    fun onContractInviteToggle(enabled: Boolean) {
        _uiState.update { it.copy(contractInviteEnabled = enabled) }
        savePreferences()
    }

    fun onContractStatusToggle(enabled: Boolean) {
        _uiState.update { it.copy(contractStatusEnabled = enabled) }
        savePreferences()
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}