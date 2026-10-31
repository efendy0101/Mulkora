package com.mulkora.feature.auth.presentation.roleselection

import com.google.common.truth.Truth.assertThat
import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.auth.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * RoleSelectionViewModelTest — Rol seçim ekranı senaryoları.
 *
 * CLAUDE.md §3: Kullanıcı rol seçer (kiracı / ev sahibi).
 *   - Rol, kullanıcı tarafından değiştirilemez.
 *   - Rol seçimi kayıt akışında zorunludur.
 * docs/21-testing-strategy.md — UT-AUTH kapsamı.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoleSelectionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: RoleSelectionViewModel

    @Before
    fun setup() {
        authRepository = mockk()
        viewModel = RoleSelectionViewModel(authRepository)
    }

    // ============================================================
    // Başlangıç Durumu
    // ============================================================

    @Test
    fun `başlangıçta seçili rol null ve hata yok`() {
        val state = viewModel.uiState.value
        assertThat(state.selectedRole).isNull()
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    // ============================================================
    // Rol Seçimi
    // ============================================================

    @Test
    fun `onRoleSelected kiracı rolünü seçer`() {
        viewModel.onRoleSelected("tenant")
        assertThat(viewModel.uiState.value.selectedRole).isEqualTo("tenant")
    }

    @Test
    fun `onRoleSelected ev sahibi rolünü seçer`() {
        viewModel.onRoleSelected("landlord")
        assertThat(viewModel.uiState.value.selectedRole).isEqualTo("landlord")
    }

    @Test
    fun `rol değiştirildiğinde selectedRole güncellenir`() {
        viewModel.onRoleSelected("tenant")
        viewModel.onRoleSelected("landlord")
        assertThat(viewModel.uiState.value.selectedRole).isEqualTo("landlord")
    }

    // ============================================================
    // Rol Kaydetme — Başarılı
    // ============================================================

    @Test
    fun `confirmRole seçili rol ile başarıyla kaydedilir`() = runTest {
        coEvery {
            authRepository.setUserRole("tenant")
        } returns Result.Success(Unit)

        viewModel.onRoleSelected("tenant")
        viewModel.confirmRole()

        val state = viewModel.uiState.value
        assertThat(state.isRoleConfirmed).isTrue()
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    // ============================================================
    // Rol Kaydetme — Hata
    // ============================================================

    @Test
    fun `confirmRole hata verdiğinde errorMessage ayarlanır`() = runTest {
        coEvery {
            authRepository.setUserRole(any())
        } returns Result.Error(Exception("Rol kaydedilemedi"))

        viewModel.onRoleSelected("tenant")
        viewModel.confirmRole()

        val state = viewModel.uiState.value
        assertThat(state.isRoleConfirmed).isFalse()
        assertThat(state.errorMessage).isNotNull()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `rol seçilmeden confirmRole çağrıldığında hata üretilir`() = runTest {
        viewModel.confirmRole()

        val state = viewModel.uiState.value
        assertThat(state.isRoleConfirmed).isFalse()
        assertThat(state.errorMessage).isNotNull()
    }
}