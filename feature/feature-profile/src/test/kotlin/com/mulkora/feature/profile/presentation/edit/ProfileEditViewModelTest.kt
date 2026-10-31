package com.mulkora.feature.profile.presentation.edit

import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.profile.domain.repository.ProfileRepository
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
 * ProfileEditViewModel birim testleri.
 *
 * CLAUDE.md §6: Profil yönetimi — kullanıcı adı bu ekranda değiştirilemez.
 * docs/21-testing-strategy.md — UT-PROFILE-001 ile UT-PROFILE-008.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var profileRepository: ProfileRepository
    private lateinit var viewModel: ProfileEditViewModel

    @Before
    fun setUp() {
        profileRepository = mockk()
        viewModel = ProfileEditViewModel(profileRepository)
    }

    // ─── UT-PROFILE-001: Başlangıç durumu ────────────────────────────────────

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertEquals("", state.displayName)
        assertEquals("", state.bio)
        assertEquals("", state.city)
        assertNull(state.photoUrl)
        assertFalse(state.isLoading)
        assertFalse(state.isSaving)
        assertFalse(state.isUploadingPhoto)
        assertNull(state.error)
    }

    // ─── UT-PROFILE-002: Display name değişimi ───────────────────────────────

    @Test
    fun `onDisplayNameChange updates displayName`() {
        viewModel.onDisplayNameChange("Yeni İsim")
        assertEquals("Yeni İsim", viewModel.uiState.value.displayName)
    }

    // ─── UT-PROFILE-003: Bio max uzunluk ─────────────────────────────────────

    @Test
    fun `onBioChange truncates to BIO_MAX_LENGTH`() {
        val longBio = "a".repeat(300)  // BIO_MAX_LENGTH = 200
        viewModel.onBioChange(longBio)
        assertEquals(ProfileEditViewModel.BIO_MAX_LENGTH, viewModel.uiState.value.bio.length)
    }

    // ─── UT-PROFILE-004: Şehir değişimi ──────────────────────────────────────

    @Test
    fun `onCityChange updates city`() {
        viewModel.onCityChange("İstanbul")
        assertEquals("İstanbul", viewModel.uiState.value.city)
    }

    // ─── UT-PROFILE-005: Boş display name kaydedilmez ────────────────────────

    @Test
    fun `onSaveClicked with empty displayName sets error`() = runTest {
        viewModel.onDisplayNameChange("")
        var callbackCalled = false
        viewModel.onSaveClicked(onSuccess = { callbackCalled = true })

        assertFalse(callbackCalled)
        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    // ─── UT-PROFILE-006: Başarılı kaydetme ───────────────────────────────────

    @Test
    fun `onSaveClicked success invokes callback`() = runTest {
        viewModel.onDisplayNameChange("Geçerli İsim")
        viewModel.onBioChange("Bir biyografi")
        viewModel.onCityChange("Ankara")

        coEvery {
            profileRepository.updateProfile(any())
        } returns Result.Success(Unit)

        var callbackCalled = false
        viewModel.onSaveClicked(onSuccess = { callbackCalled = true })

        assertTrue(callbackCalled)
        assertFalse(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.error)
    }

    // ─── UT-PROFILE-007: Kaydetme başarısız ──────────────────────────────────

    @Test
    fun `onSaveClicked failure sets error`() = runTest {
        viewModel.onDisplayNameChange("Geçerli İsim")

        coEvery {
            profileRepository.updateProfile(any())
        } returns Result.Failure(Exception("Güncelleme başarısız"))

        var callbackCalled = false
        viewModel.onSaveClicked(onSuccess = { callbackCalled = true })

        assertFalse(callbackCalled)
        assertFalse(viewModel.uiState.value.isSaving)
        assertNotNull(viewModel.uiState.value.error)
    }

    // ─── UT-PROFILE-008: Bio tam sınır değeri ────────────────────────────────

    @Test
    fun `onBioChange accepts exactly BIO_MAX_LENGTH characters`() {
        val exactBio = "a".repeat(ProfileEditViewModel.BIO_MAX_LENGTH)
        viewModel.onBioChange(exactBio)
        assertEquals(ProfileEditViewModel.BIO_MAX_LENGTH, viewModel.uiState.value.bio.length)
    }
}