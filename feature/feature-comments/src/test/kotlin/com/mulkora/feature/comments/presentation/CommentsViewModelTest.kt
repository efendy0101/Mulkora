package com.mulkora.feature.comments.presentation

import androidx.lifecycle.SavedStateHandle
import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.comments.domain.model.Comment
import com.mulkora.feature.comments.domain.repository.CommentsRepository
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
import java.util.Date

/**
 * CommentsViewModel birim testleri.
 *
 * CLAUDE.md §9: Yorum ve Cevap Sistemi.
 * docs/21-testing-strategy.md — UT-COMMENT-001 ile UT-COMMENT-008.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CommentsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var commentsRepository: CommentsRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: CommentsViewModel

    private val testPostId = "post_test_456"

    private fun makeComment(id: String, text: String = "Test yorum") = Comment(
        id = id,
        postId = testPostId,
        authorId = "user_001",
        authorName = "Test Kullanıcı",
        authorPhotoUrl = null,
        text = text,
        createdAt = Date(),
        likeCount = 0,
        isLikedByMe = false,
        isOwner = false
    )

    @Before
    fun setUp() {
        commentsRepository = mockk()
        savedStateHandle = SavedStateHandle(mapOf("postId" to testPostId))

        // Varsayılan: boş yorum listesi
        every {
            commentsRepository.observeComments(testPostId)
        } returns flowOf(emptyList())

        viewModel = CommentsViewModel(commentsRepository, savedStateHandle)
    }

    // ─── UT-COMMENT-001: Başlangıç durumu ────────────────────────────────────

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertTrue(state.comments.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isSubmitting)
        assertNull(state.error)
        assertEquals("", state.inputText)
    }

    // ─── UT-COMMENT-002: Yorumlar yükleme ────────────────────────────────────

    @Test
    fun `comments loaded from repository reflected in state`() = runTest {
        val comments = listOf(
            makeComment("c1", "İlk yorum"),
            makeComment("c2", "İkinci yorum")
        )

        every {
            commentsRepository.observeComments(testPostId)
        } returns flowOf(comments)

        val vm = CommentsViewModel(commentsRepository, savedStateHandle)

        assertEquals(2, vm.uiState.value.comments.size)
    }

    // ─── UT-COMMENT-003: Input değişimi ──────────────────────────────────────

    @Test
    fun `onInputChange updates inputText`() {
        viewModel.onInputChange("Merhaba dünya")
        assertEquals("Merhaba dünya", viewModel.uiState.value.inputText)
    }

    // ─── UT-COMMENT-004: Boş yorum gönderme engellenir ───────────────────────

    @Test
    fun `addComment with empty text does nothing`() = runTest {
        viewModel.onInputChange("")
        viewModel.addComment()

        // Boş yorum gönderilmemeli; isSubmitting false kalmalı
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    // ─── UT-COMMENT-005: Başarılı yorum ekleme ───────────────────────────────

    @Test
    fun `addComment success clears input`() = runTest {
        coEvery {
            commentsRepository.addComment(testPostId, "Harika bir gönderi!")
        } returns Result.Success(Unit)

        viewModel.onInputChange("Harika bir gönderi!")
        viewModel.addComment()

        val state = viewModel.uiState.value
        assertEquals("", state.inputText)
        assertFalse(state.isSubmitting)
        assertNull(state.error)
    }

    // ─── UT-COMMENT-006: Yorum ekleme başarısız ──────────────────────────────

    @Test
    fun `addComment failure sets error`() = runTest {
        coEvery {
            commentsRepository.addComment(testPostId, "Bir yorum")
        } returns Result.Failure(Exception("Sunucu hatası"))

        viewModel.onInputChange("Bir yorum")
        viewModel.addComment()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertNotNull(state.error)
    }

    // ─── UT-COMMENT-007: Yorum silme başarılı ────────────────────────────────

    @Test
    fun `deleteComment success clears error`() = runTest {
        coEvery {
            commentsRepository.deleteComment(testPostId, "c1")
        } returns Result.Success(Unit)

        viewModel.deleteComment("c1")

        assertNull(viewModel.uiState.value.error)
    }

    // ─── UT-COMMENT-008: Yorum silme başarısız ───────────────────────────────

    @Test
    fun `deleteComment failure sets error`() = runTest {
        coEvery {
            commentsRepository.deleteComment(testPostId, "c1")
        } returns Result.Failure(Exception("Silme başarısız"))

        viewModel.deleteComment("c1")

        assertNotNull(viewModel.uiState.value.error)
    }
}