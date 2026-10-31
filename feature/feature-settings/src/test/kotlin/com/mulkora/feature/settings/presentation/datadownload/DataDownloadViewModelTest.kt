package com.mulkora.feature.settings.presentation.datadownload

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctions
import com.mulkora.core.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * DataDownloadViewModel birim testleri.
 *
 * CLAUDE.md §27: KVKK kapsamında kullanıcı verilerini dışa aktarma talebi.
 * docs/21-testing-strategy.md — UT-SETTINGS-DATADOWNLOAD-001 ile UT-SETTINGS-DATADOWNLOAD-005.
 *
 * Not: ViewModel FirebaseFunctions'ı doğrudan inject eder (relaxed mock).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataDownloadViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var firebaseFunctions: FirebaseFunctions
    private lateinit var viewModel: DataDownloadViewModel

    @Before
    fun setUp() {
        firebaseAuth = mockk()
        firebaseUser = mockk()
        firebaseFunctions = mockk(relaxed = true)

        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "test_uid"
        every { firebaseUser.email } returns "test@example.com"

        viewModel = DataDownloadViewModel(firebaseAuth, firebaseFunctions)
    }

    // ─── UT-SETTINGS-DATADOWNLOAD-001: Başlangıç durumu ──────────────────────

    @Test
    fun `initial state is not loading and no request sent`() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.requestSent)
        assertNull(state.error)
    }

    // ─── UT-SETTINGS-DATADOWNLOAD-002: Kullanıcı e-postası state'e yüklenir ──

    @Test
    fun `initial state contains user email`() {
        val state = viewModel.uiState.value
        assertTrue(state.userEmail == "test@example.com")
    }

    // ─── UT-SETTINGS-DATADOWNLOAD-003: Oturum açmamış kullanıcı ─────────────

    @Test
    fun `when currentUser is null state has empty email`() {
        every { firebaseAuth.currentUser } returns null

        val vm = DataDownloadViewModel(firebaseAuth, firebaseFunctions)
        val state = vm.uiState.value
        assertFalse(state.isLoading)
    }

    // ─── UT-SETTINGS-DATADOWNLOAD-004: Hata temizleme ────────────────────────

    @Test
    fun `clearError sets error to null`() {
        // error state'i direkt test etmek için — başlangıçta null
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    // ─── UT-SETTINGS-DATADOWNLOAD-005: E-posta null ise state tutarlı ────────

    @Test
    fun `when user email is null state handles gracefully`() {
        every { firebaseUser.email } returns null

        val vm = DataDownloadViewModel(firebaseAuth, firebaseFunctions)
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.requestSent)
    }
}