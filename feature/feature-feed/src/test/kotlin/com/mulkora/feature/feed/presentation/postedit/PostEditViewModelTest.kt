package com.mulkora.feature.feed.presentation.postedit

import androidx.lifecycle.SavedStateHandle
import com.google.firebase.functions.FirebaseFunctions
import com.mulkora.core.testing.MainDispatcherRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * PostEditViewModel birim testleri.
 *
 * CLAUDE.md §6: Gönderi yalnızca edit_requested durumundayken düzenlenebilir.
 * docs/17-post-moderation-state-machine.md: edit_requested → pending_approval geçişi.
 * docs/21-testing-strategy.md — UT-POSTEDIT-001 ile UT-POSTEDIT-008.
 *
 * Not: ViewModel FirebaseFunctions'ı doğrudan inject eder.
 * Firebase çağrıları relaxed mock ile simüle edilir; form doğrulama mantığı test edilir.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PostEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var firebaseFunctions: FirebaseFunctions
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: PostEditViewModel

    private val testPostId = "post_edit_001"

    @Before
    fun setUp() {
        firebaseFunctions = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("postId" to testPostId))
        viewModel = PostEditViewModel(savedStateHandle, firebaseFunctions)
    }

    // ─── UT-POSTEDIT-001: Başlangıç durumu ───────────────────────────────────

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.submitted)
        assertNull(state.errorMessage)
    }

    // ─── UT-POSTEDIT-002: Başlık değişimi ────────────────────────────────────

    @Test
    fun `onTitleChanged updates title`() {
        viewModel.onTitleChanged("Yeni Başlık")
        assertTrue(viewModel.uiState.value.title == "Yeni Başlık")
    }

    // ─── UT-POSTEDIT-003: İçerik değişimi ────────────────────────────────────

    @Test
    fun `onContentChanged updates content`() {
        viewModel.onContentChanged("Bu bir test içeriğidir.")
        assertTrue(viewModel.uiState.value.content == "Bu bir test içeriğidir.")
    }

    // ─── UT-POSTEDIT-004: Geçerli form doğrulama ─────────────────────────────

    @Test
    fun `isFormValid is true when title and content meet criteria`() {
        // title: 3..150 karakter, content: boş olmayan
        viewModel.onTitleChanged("Geçerli Başlık")
        viewModel.onContentChanged("Bu geçerli bir içeriktir, yeterince uzundur.")

        assertTrue(viewModel.uiState.value.isFormValid)
    }

    // ─── UT-POSTEDIT-005: Kısa başlık geçersiz form ──────────────────────────

    @Test
    fun `isFormValid is false when title is too short`() {
        viewModel.onTitleChanged("AB") // 2 karakter, < 3
        viewModel.onContentChanged("Geçerli içerik var.")

        assertFalse(viewModel.uiState.value.isFormValid)
    }

    // ─── UT-POSTEDIT-006: Boş içerik geçersiz form ───────────────────────────

    @Test
    fun `isFormValid is false when content is empty`() {
        viewModel.onTitleChanged("Geçerli Başlık")
        viewModel.onContentChanged("")

        assertFalse(viewModel.uiState.value.isFormValid)
    }

    // ─── UT-POSTEDIT-007: Çok uzun başlık geçersiz form ──────────────────────

    @Test
    fun `isFormValid is false when title exceeds 150 characters`() {
        val longTitle = "a".repeat(151)
        viewModel.onTitleChanged(longTitle)
        viewModel.onContentChanged("Geçerli içerik.")

        assertFalse(viewModel.uiState.value.isFormValid)
    }

    // ─── UT-POSTEDIT-008: Tam sınır başlık geçerli form ──────────────────────

    @Test
    fun `isFormValid is true when title is exactly 150 characters`() {
        val exactTitle = "a".repeat(150)
        viewModel.onTitleChanged(exactTitle)
        viewModel.onContentChanged("Geçerli içerik.")

        assertTrue(viewModel.uiState.value.isFormValid)
    }
}