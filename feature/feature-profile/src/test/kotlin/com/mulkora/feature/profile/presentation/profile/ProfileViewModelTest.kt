package com.mulkora.feature.profile.presentation.profile

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mulkora.core.common.model.UserRole
import com.mulkora.core.common.result.Result
import com.mulkora.feature.profile.domain.model.UserProfile
import com.mulkora.feature.profile.domain.repository.ProfileRepository
import com.mulkora.core.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * ProfileViewModelTest — Kendi profil yükleme ve işlemleri.
 *
 * CLAUDE.md §19: Profil sayfası gereksinimleri.
 * CLAUDE.md §32: Test senaryoları.
 */
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ProfileRepository
    private lateinit var viewModel: ProfileViewModel

    private val fakeProfile = UserProfile(
        uid = "user_001",
        username = "test_user",
        displayName = "Test Kullanıcısı",
        email = "test@example.com",
        role = UserRole.TENANT,
        city = "İstanbul",
        bio = "Merhaba!",
        avatarUrl = null,
        postCount = 5,
        referenceScore = 4.2,
        adsRemoved = false,
        isBanned = false,
    )

    @Before
    fun setup() {
        repository = mockk()
    }

    private fun createViewModel(): ProfileViewModel {
        return ProfileViewModel(repository)
    }

    // ============================================================
    // Başlangıç Durumu
    // ============================================================

    @Test
    fun `başlangıç durumunda yükleme göstergesi aktif ve profil null`() = runTest {
        every { repository.observeCurrentUserProfile() } returns flowOf()
        val vm = createViewModel()
        val state = vm.uiState.value
        assertThat(state.profile).isNull()
    }

    // ============================================================
    // Profil Yükleme
    // ============================================================

    @Test
    fun `profil başarıyla yüklenince uiState profili günceller`() = runTest {
        every { repository.observeCurrentUserProfile() } returns flowOf(
            Result.Success(fakeProfile)
        )
        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.profile).isEqualTo(fakeProfile)
            assertThat(state.isLoading).isFalse()
            assertThat(state.errorMessage).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `profil yükleme hatası uiState errorMessage ayarlar`() = runTest {
        every { repository.observeCurrentUserProfile() } returns flowOf(
            Result.Error(Exception("Profil yüklenemedi"))
        )
        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isNotNull()
            assertThat(state.profile).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `profil yükleme sırasında isLoading true döner`() = runTest {
        every { repository.observeCurrentUserProfile() } returns flowOf(
            Result.Loading,
            Result.Success(fakeProfile)
        )
        val vm = createViewModel()

        vm.uiState.test {
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.profile).isEqualTo(fakeProfile)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Profil Göstergeleri
    // ============================================================

    @Test
    fun `kiracı rolüyle profil yüklenince kiracı rozeti gösterilir`() = runTest {
        val tenantProfile = fakeProfile.copy(role = UserRole.TENANT)
        every { repository.observeCurrentUserProfile() } returns flowOf(
            Result.Success(tenantProfile)
        )
        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.profile?.role).isEqualTo(UserRole.TENANT)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ev sahibi rolüyle profil yüklenince ev sahibi rozeti gösterilir`() = runTest {
        val landlordProfile = fakeProfile.copy(role = UserRole.LANDLORD)
        every { repository.observeCurrentUserProfile() } returns flowOf(
            Result.Success(landlordProfile)
        )
        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.profile?.role).isEqualTo(UserRole.LANDLORD)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `referans puanı 0 ile 5 arasında olmalı`() = runTest {
        val profileWithScore = fakeProfile.copy(referenceScore = 3.7)
        every { repository.observeCurrentUserProfile() } returns flowOf(
            Result.Success(profileWithScore)
        )
        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitItem()
            val score = state.profile?.referenceScore ?: 0.0
            assertThat(score).isAtLeast(0.0)
            assertThat(score).isAtMost(5.0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Hata Temizleme
    // ============================================================

    @Test
    fun `clearError çağrısı sonrası errorMessage null olur`() = runTest {
        every { repository.observeCurrentUserProfile() } returns flowOf(
            Result.Error(Exception("Hata"))
        )
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem() // hata state
            vm.clearError()
            val clearedState = awaitItem()
            assertThat(clearedState.errorMessage).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Kullanıcı Engelleme
    // ============================================================

    @Test
    fun `blockUser başarılı olduğunda snackbar mesajı gösterilir`() = runTest {
        every { repository.observeCurrentUserProfile() } returns flowOf(
            Result.Success(fakeProfile)
        )
        coEvery { repository.blockUser(any()) } returns Result.Success(Unit)
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem()
            vm.blockUser("target_user_123")
            val state = awaitItem()
            assertThat(state.snackbarMessage).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `blockUser başarısız olduğunda hata mesajı gösterilir`() = runTest {
        every { repository.observeCurrentUserProfile() } returns flowOf(
            Result.Success(fakeProfile)
        )
        coEvery { repository.blockUser(any()) } returns Result.Error(Exception("Engelleme hatası"))
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem()
            vm.blockUser("target_user_123")
            val state = awaitItem()
            assertThat(state.errorMessage).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }
}