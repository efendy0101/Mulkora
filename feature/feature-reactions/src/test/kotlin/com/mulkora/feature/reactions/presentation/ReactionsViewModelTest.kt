package com.mulkora.feature.reactions.presentation

import androidx.lifecycle.SavedStateHandle
import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.reactions.domain.model.PostReaction
import com.mulkora.feature.reactions.domain.model.ReactionType
import com.mulkora.feature.reactions.domain.repository.ReactionsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
 * ReactionsViewModel birim testleri.
 *
 * CLAUDE.md §10: Beğeni/eksi/puan sayaçları istemci tarafından değiştirilemez.
 * Tüm yazma işlemleri Cloud Functions üzerinden yapılır.
 * docs/21-testing-strategy.md — UT-REACT-001 ile UT-REACT-007.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReactionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var reactionsRepository: ReactionsRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: ReactionsViewModel

    private val testPostId = "test_post_123"

    @Before
    fun setUp() {
        reactionsRepository = mockk()
        savedStateHandle = SavedStateHandle(mapOf("postId" to testPostId))

        // Varsayılan: boş reaksiyon akışı
        every {
            reactionsRepository.observePostReaction(testPostId)
        } returns flowOf(null)

        viewModel = ReactionsViewModel(savedStateHandle, reactionsRepository)
    }

    // ─── UT-REACT-001: Başlangıç durumu ──────────────────────────────────────

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isActionLoading)
        assertNull(state.error)
    }

    // ─── UT-REACT-002: Mevcut reaksiyon yükleme ──────────────────────────────

    @Test
    fun `reaction loaded from repository is reflected in state`() = runTest {
        val existingReaction = PostReaction(
            postId = testPostId,
            userId = "user_abc",
            type = ReactionType.LIKE,
            rating = null
        )

        every {
            reactionsRepository.observePostReaction(testPostId)
        } returns flowOf(existingReaction)

        val vm = ReactionsViewModel(savedStateHandle, reactionsRepository)

        val state = vm.uiState.value
        assertEquals(existingReaction, state.reaction)
    }

    // ─── UT-REACT-003: Beğeni toggle — başarılı ──────────────────────────────

    @Test
    fun `toggleLike success clears actionLoading`() = runTest {
        coEvery {
            reactionsRepository.toggleReaction(testPostId, ReactionType.LIKE)
        } returns Result.Success(Unit)

        viewModel.toggleLike()

        val state = viewModel.uiState.value
        assertFalse(state.isActionLoading)
        assertNull(state.error)
    }

    // ─── UT-REACT-004: Beğeni toggle — başarısız ─────────────────────────────

    @Test
    fun `toggleLike failure sets error`() = runTest {
        coEvery {
            reactionsRepository.toggleReaction(testPostId, ReactionType.LIKE)
        } returns Result.Failure(Exception("Ağ hatası"))

        viewModel.toggleLike()

        val state = viewModel.uiState.value
        assertFalse(state.isActionLoading)
        assertNotNull(state.error)
    }

    // ─── UT-REACT-005: Eksi oy toggle — başarılı ─────────────────────────────

    @Test
    fun `toggleDislike success clears actionLoading`() = runTest {
        coEvery {
            reactionsRepository.toggleReaction(testPostId, ReactionType.DISLIKE)
        } returns Result.Success(Unit)

        viewModel.toggleDislike()

        val state = viewModel.uiState.value
        assertFalse(state.isActionLoading)
        assertNull(state.error)
    }

    // ─── UT-REACT-006: Puan verme — başarılı ─────────────────────────────────

    @Test
    fun `submitRating success clears actionLoading`() = runTest {
        coEvery {
            reactionsRepository.submitRating(testPostId, 4)
        } returns Result.Success(Unit)

        viewModel.submitRating(4)

        val state = viewModel.uiState.value
        assertFalse(state.isActionLoading)
        assertNull(state.error)
    }

    // ─── UT-REACT-007: Puan verme — geçersiz aralık ──────────────────────────

    @Test
    fun `submitRating with invalid score sets error`() = runTest {
        // 1-5 dışında puan geçersizdir
        viewModel.submitRating(0)

        val state = viewModel.uiState.value
        assertNotNull(state.error)
    }

    // ─── UT-REACT-008: clearError çalışması ──────────────────────────────────

    @Test
    fun `clearError resets error state`() = runTest {
        coEvery {
            reactionsRepository.toggleReaction(testPostId, ReactionType.LIKE)
        } returns Result.Failure(Exception("Hata"))

        viewModel.toggleLike()
        assertNotNull(viewModel.uiState.value.error)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }
}