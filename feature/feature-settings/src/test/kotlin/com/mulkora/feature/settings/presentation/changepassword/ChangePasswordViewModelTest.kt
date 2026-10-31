package com.mulkora.feature.settings.presentation.changepassword

import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.auth.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * ChangePasswordViewModel birim testleri.
 *
 * CLAUDE.md §26: Şifre değiştirme — AuthRepository.changePassword() çağrısı.
 * docs/21-testing-strategy.md — UT-SETTINGS-PASSWORD-001 ile UT-SETTINGS-PASSWORD-007.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChangePasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: ChangePasswordViewModel

    @Before
    fun setUp() {
        authRepository = mockk(relaxed = true)
        viewModel = ChangePasswordViewModel(authRepository)
    }

    // ─── UT-SETTINGS-PASSWORD-001: Başlangıç durumu ──────────────────────────

    @Test
    fun `initial state has empty fields and no error`() {
        val state = viewModel.uiState.value
        assertTrue(state.currentPassword.isEmpty())
        assertTrue(state.newPassword.isEmpty())
        assertTrue(state.confirmPassword.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertNull(state.error)
    }

    // ─── UT-SETTINGS-PASSWORD-002: Alan güncellemeleri ────────────────────────

    @Test
    fun `onCurrentPasswordChange updates currentPassword`() {
        viewModel.onCurrentPasswordChange("oldPass123")
        assertEquals("oldPass123", viewModel.uiState.value.currentPassword)
    }

    @Test
    fun `onNewPasswordChange updates newPassword`() {
        viewModel.onNewPasswordChange("newPass123")
        assertEquals("newPass123", viewModel.uiState.value.newPassword)
    }

    @Test
    fun `onConfirmPasswordChange updates confirmPassword`() {
        viewModel.onConfirmPasswordChange("newPass123")
        assertEquals("newPass123", viewModel.uiState.value.confirmPassword)
    }

    // ─── UT-SETTINGS-PASSWORD-003: Boş alan validasyonu ─────────────────────

    @Test
    fun `onChangePasswordClicked with empty fields sets error`() = runTest {
        viewModel.onChangePasswordClicked(onSuccess = {})
        assertNotNull(viewModel.uiState.value.error)
    }

    // ─── UT-SETTINGS-PASSWORD-004: Şifre uzunluk kontrolü ───────────────────

    @Test
    fun `onChangePasswordClicked with short new password sets error`() = runTest {
        viewModel.onCurrentPasswordChange("oldPass123")
        viewModel.onNewPasswordChange("short")
        viewModel.onConfirmPasswordChange("short")
        viewModel.onChangePasswordClicked(onSuccess = {})
        assertNotNull(viewModel.uiState.value.error)
    }

    // ─── UT-SETTINGS-PASSWORD-005: Şifre eşleşme kontrolü ───────────────────

    @Test
    fun `onChangePasswordClicked with mismatched passwords sets error`() = runTest {
        viewModel.onCurrentPasswordChange("oldPass123")
        viewModel.onNewPasswordChange("newPass123")
        viewModel.onConfirmPasswordChange("differentPass123")
        viewModel.onChangePasswordClicked(onSuccess = {})
        assertNotNull(viewModel.uiState.value.error)
    }

    // ─── UT-SETTINGS-PASSWORD-006: Aynı şifre kontrolü ──────────────────────

    @Test
    fun `onChangePasswordClicked with same old and new password sets error`() = runTest {
        viewModel.onCurrentPasswordChange("samePass123")
        viewModel.onNewPasswordChange("samePass123")
        viewModel.onConfirmPasswordChange("samePass123")
        viewModel.onChangePasswordClicked(onSuccess = {})
        assertNotNull(viewModel.uiState.value.error)
    }

    // ─── UT-SETTINGS-PASSWORD-007: Başarılı değişiklik ──────────────────────

    @Test
    fun `onChangePasswordClicked success sets isSuccess true`() = runTest {
        coEvery { authRepository.changePassword(any(), any()) } returns Unit
        viewModel.onCurrentPasswordChange("oldPass123")
        viewModel.onNewPasswordChange("newPass456")
        viewModel.onConfirmPasswordChange("newPass456")
        var successCalled = false
        viewModel.onChangePasswordClicked(onSuccess = { successCalled = true })
        assertTrue(successCalled || viewModel.uiState.value.isSuccess)
    }
}