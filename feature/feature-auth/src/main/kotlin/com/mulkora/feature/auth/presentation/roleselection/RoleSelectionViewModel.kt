package com.mulkora.feature.auth.presentation.roleselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mulkora.core.common.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Rol seçim ekranı ViewModel.
 * CLAUDE.md §3: Kullanıcı TENANT, LANDLORD veya BOTH rolünden birini seçer.
 * CLAUDE.md §26: Kullanıcı kendi rolünü moderatör/admin yapamaz.
 *
 * Seçilebilir roller yalnızca TENANT, LANDLORD, BOTH — moderatör ve admin seçilemez.
 */
@HiltViewModel
class RoleSelectionViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoleSelectionUiState())
    val uiState: StateFlow<RoleSelectionUiState> = _uiState.asStateFlow()

    /** CLAUDE.md §3: Yalnızca bu üç rol kullanıcı tarafından seçilebilir. */
    val selectableRoles = listOf(UserRole.TENANT, UserRole.LANDLORD, UserRole.BOTH)

    fun onRoleSelected(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun onContinueClicked(onSuccess: () -> Unit) {
        val role = _uiState.value.selectedRole ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val uid = firebaseAuth.currentUser?.uid
                    ?: throw IllegalStateException("Oturum açık değil")
                firestore.collection("users").document(uid)
                    .update("role", role.value)
                    .await()
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Rol kaydedilemedi. Lütfen tekrar deneyin.")
                }
            }
        }
    }
}

data class RoleSelectionUiState(
    val selectedRole: UserRole? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)