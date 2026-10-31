package com.mulkora.feature.auth.presentation.termsacceptance

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
 * KVKK ve hukuki onaylar ViewModel.
 * CLAUDE.md §4: Kayıt sırasında alınması zorunlu onaylar:
 *  - Kullanım koşulları
 *  - Gizlilik politikası
 *  - KVKK aydınlatma metni
 *  - Topluluk kuralları
 *  - 18 yaşından büyük olunduğuna dair onay
 *
 * CLAUDE.md §27: Açık rıza ile zorunlu veri işleme süreçleri ayrılmalıdır.
 */
@HiltViewModel
class TermsAcceptanceViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TermsAcceptanceUiState())
    val uiState: StateFlow<TermsAcceptanceUiState> = _uiState.asStateFlow()

    val allRequiredAccepted: Boolean
        get() = with(_uiState.value) {
            termsAccepted && privacyAccepted && kvkkAccepted && communityRulesAccepted && ageConfirmed
        }

    fun onTermsChanged(accepted: Boolean) {
        _uiState.update { it.copy(termsAccepted = accepted) }
    }

    fun onPrivacyChanged(accepted: Boolean) {
        _uiState.update { it.copy(privacyAccepted = accepted) }
    }

    fun onKvkkChanged(accepted: Boolean) {
        _uiState.update { it.copy(kvkkAccepted = accepted) }
    }

    fun onCommunityRulesChanged(accepted: Boolean) {
        _uiState.update { it.copy(communityRulesAccepted = accepted) }
    }

    fun onAgeConfirmedChanged(confirmed: Boolean) {
        _uiState.update { it.copy(ageConfirmed = confirmed) }
    }

    fun onAcceptAll(onSuccess: () -> Unit) {
        if (!allRequiredAccepted) {
            _uiState.update { it.copy(error = "Devam etmek için tüm onayları vermeniz gerekmektedir.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val uid = firebaseAuth.currentUser?.uid
                    ?: throw IllegalStateException("Oturum açık değil")
                // Onay kayıtlarını Firestore'a yaz
                firestore.collection("users").document(uid).update(
                    mapOf(
                        "termsAccepted" to true,
                        "privacyAccepted" to true,
                        "kvkkAccepted" to true,
                        "communityRulesAccepted" to true,
                        "ageConfirmed" to true,
                        "termsAcceptedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "onboardingCompleted" to true
                    )
                ).await()
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Onaylar kaydedilemedi. Lütfen tekrar deneyin.")
                }
            }
        }
    }
}

data class TermsAcceptanceUiState(
    val termsAccepted: Boolean = false,
    val privacyAccepted: Boolean = false,
    val kvkkAccepted: Boolean = false,
    val communityRulesAccepted: Boolean = false,
    val ageConfirmed: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)