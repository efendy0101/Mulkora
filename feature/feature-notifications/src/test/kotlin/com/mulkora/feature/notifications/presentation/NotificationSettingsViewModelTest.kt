package com.mulkora.feature.notifications.presentation

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.mulkora.core.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * NotificationSettingsViewModel birim testleri.
 *
 * CLAUDE.md §19: Bildirim Sistemi — kullanıcı bildirim tercihleri.
 * docs/21-testing-strategy.md — UT-NOTIF-001 ile UT-NOTIF-006.
 *
 * Not: ViewModel FirebaseAuth ve FirebaseFirestore'u doğrudan inject eder.
 * Firestore çağrıları relaxed mock ile simüle edilir; durum makinesi davranışı test edilir.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var viewModel: NotificationSettingsViewModel

    @Before
    fun setUp() {
        firebaseAuth = mockk()
        firebaseUser = mockk()
        firebaseFirestore = mockk(relaxed = true)

        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "test_user_uid"

        viewModel = NotificationSettingsViewModel(firebaseAuth, firebaseFirestore)
    }

    // ─── UT-NOTIF-001: Başlangıç durumu ──────────────────────────────────────

    @Test
    fun `initial state is not loading and has no error`() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    // ─── UT-NOTIF-002: Yorum bildirimleri toggle ──────────────────────────────

    @Test
    fun `onCommentNotificationsToggled updates state correctly`() {
        val initialValue = viewModel.uiState.value.commentsEnabled
        viewModel.onCommentNotificationsToggled(!initialValue)
        // Değer değişmeli
        assertTrue(viewModel.uiState.value.commentsEnabled != initialValue)
    }

    // ─── UT-NOTIF-003: Beğeni bildirimleri toggle ─────────────────────────────

    @Test
    fun `onReactionNotificationsToggled updates state correctly`() {
        val initialValue = viewModel.uiState.value.reactionsEnabled
        viewModel.onReactionNotificationsToggled(!initialValue)
        assertTrue(viewModel.uiState.value.reactionsEnabled != initialValue)
    }

    // ─── UT-NOTIF-004: Referans bildirimleri toggle ───────────────────────────

    @Test
    fun `onReferenceNotificationsToggled updates state correctly`() {
        val initialValue = viewModel.uiState.value.referencesEnabled
        viewModel.onReferenceNotificationsToggled(!initialValue)
        assertTrue(viewModel.uiState.value.referencesEnabled != initialValue)
    }

    // ─── UT-NOTIF-005: Sözleşme bildirimleri toggle ───────────────────────────

    @Test
    fun `onContractNotificationsToggled updates state correctly`() {
        val initialValue = viewModel.uiState.value.contractsEnabled
        viewModel.onContractNotificationsToggled(!initialValue)
        assertTrue(viewModel.uiState.value.contractsEnabled != initialValue)
    }

    // ─── UT-NOTIF-006: Kullanıcı oturum açmamış ──────────────────────────────

    @Test
    fun `when currentUser is null ViewModel handles gracefully`() {
        every { firebaseAuth.currentUser } returns null

        val vm = NotificationSettingsViewModel(firebaseAuth, firebaseFirestore)
        // Oturum açık olmadan ViewModel hata veya boş state ile başlamalı
        assertFalse(vm.uiState.value.isLoading)
    }
}