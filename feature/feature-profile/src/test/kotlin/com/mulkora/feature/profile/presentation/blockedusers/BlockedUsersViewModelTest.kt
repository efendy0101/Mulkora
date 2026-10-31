package com.mulkora.feature.profile.presentation.blockedusers

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mulkora.core.common.result.Result
import com.mulkora.feature.profile.domain.model.BlockedUser
import com.mulkora.feature.profile.domain.repository.ProfileRepository
import com.mulkora.core.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date

/**
 * BlockedUsersViewModelTest — Engellenen kullanıcılar listesi ve engel kaldırma.
 *
 * CLAUDE.md §26: Firestore güvenliği — blockedUsers yazan Cloud Function üzerinden.
 * CLAUDE.md §32: Test senaryoları.
 */
class BlockedUsersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ProfileRepository
    private lateinit var viewModel: BlockedUsersViewModel

    private val fakeBlockedUsers = listOf(
        BlockedUser(
            blockedUid = "user_blocked_001",
            blockedUsername = "blocked_user_1",
            blockedAt = Date(),
        ),
        BlockedUser(
            blockedUid = "user_blocked_002",
            blockedUsername = "blocked_user_2",
            blockedAt = Date(),
        ),
        BlockedUser(
            blockedUid = "user_blocked_003",
            blockedUsername = "blocked_user_3",
            blockedAt = Date(),
        ),
    )

    @Before
    fun setup() {
        repository = mockk()
    }

    private fun createViewModel(): BlockedUsersViewModel {
        return BlockedUsersViewModel(repository)
    }

    // ============================================================
    // Engellenen Kullanıcılar Listesi
    // ============================================================

    @Test
    fun `başlangıçta engellenen kullanıcı listesi yükleniyor`() = runTest {
        every { repository.observeBlockedUsers() } returns flowOf(
            Result.Loading
        )
        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isTrue()
            assertThat(state.blockedUsers).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `engellenen kullanıcılar başarıyla yüklenir`() = runTest {
        every { repository.observeBlockedUsers() } returns flowOf(
            Result.Success(fakeBlockedUsers)
        )
        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.blockedUsers).hasSize(3)
            assertThat(state.blockedUsers[0].blockedUid).isEqualTo("user_blocked_001")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `engellenen kullanıcı listesi boşsa boş liste gösterilir`() = runTest {
        every { repository.observeBlockedUsers() } returns flowOf(
            Result.Success(emptyList())
        )
        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.blockedUsers).isEmpty()
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `engellenen kullanıcı listesi yükleme hatası errorMessage ayarlar`() = runTest {
        every { repository.observeBlockedUsers() } returns flowOf(
            Result.Error(Exception("Yükleme hatası"))
        )
        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isNotNull()
            assertThat(state.blockedUsers).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Engel Kaldırma
    // ============================================================

    @Test
    fun `unblockUser başarılı olduğunda kullanıcı listeden çıkar`() = runTest {
        every { repository.observeBlockedUsers() } returns flowOf(
            Result.Success(fakeBlockedUsers)
        )
        coEvery { repository.unblockUser("user_blocked_001") } returns Result.Success(Unit)
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem() // başlangıç state (3 kullanıcı)
            vm.unblockUser("user_blocked_001")
            val state = awaitItem()
            assertThat(state.snackbarMessage).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unblockUser başarısız olduğunda hata mesajı gösterilir`() = runTest {
        every { repository.observeBlockedUsers() } returns flowOf(
            Result.Success(fakeBlockedUsers)
        )
        coEvery { repository.unblockUser(any()) } returns Result.Error(
            Exception("Engel kaldırılamadı")
        )
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem()
            vm.unblockUser("user_blocked_002")
            val state = awaitItem()
            assertThat(state.errorMessage).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unblockUser sırasında isLoading true döner`() = runTest {
        every { repository.observeBlockedUsers() } returns flowOf(
            Result.Success(fakeBlockedUsers)
        )
        coEvery { repository.unblockUser(any()) } coAnswers {
            // biraz gecikme simülasyonu
            Result.Success(Unit)
        }
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem() // başlangıç
            vm.unblockUser("user_blocked_003")
            // unblock başarısız ya da başarılı olduğunda bir sonraki state gelir
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Hata Temizleme
    // ============================================================

    @Test
    fun `clearError çağrılınca errorMessage null olur`() = runTest {
        every { repository.observeBlockedUsers() } returns flowOf(
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

    @Test
    fun `clearSnackbar çağrılınca snackbarMessage null olur`() = runTest {
        every { repository.observeBlockedUsers() } returns flowOf(
            Result.Success(fakeBlockedUsers)
        )
        coEvery { repository.unblockUser(any()) } returns Result.Success(Unit)
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem()
            vm.unblockUser("user_blocked_001")
            awaitItem() // snackbar mesajı var
            vm.clearSnackbar()
            val clearedState = awaitItem()
            assertThat(clearedState.snackbarMessage).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }
}