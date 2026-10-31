package com.mulkora.feature.postcreate.presentation

import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.model.PostType
import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.feed.domain.repository.FeedRepository
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
 * CreatePostViewModel birim testleri.
 *
 * CLAUDE.md §28: Test kapsamı — UT-POST-001 ile UT-POST-010.
 * docs/21-testing-strategy.md: Form doğrulama, etiket, görsel, gönderi oluşturma senaryoları.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreatePostViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var feedRepository: FeedRepository
    private lateinit var viewModel: CreatePostViewModel

    @Before
    fun setUp() {
        feedRepository = mockk()
        viewModel = CreatePostViewModel(feedRepository)
    }

    // ─── UT-POST-001: Başlangıç durumu ────────────────────────────────────────

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertEquals("", state.title)
        assertEquals("", state.content)
        assertNull(state.postType)
        assertNull(state.category)
        assertTrue(state.tags.isEmpty())
        assertNull(state.imageUri)
        assertFalse(state.isLoading)
        assertNull(state.validationError)
        assertNull(state.generalError)
        assertNull(state.navigationEvent)
    }

    // ─── UT-POST-002: Başlık çok kısa ────────────────────────────────────────

    @Test
    fun `submit with title too short sets validationError`() = runTest {
        viewModel.onTitleChange("kısa")   // < 10 karakter
        viewModel.onContentChange("Bu içerik yeterince uzun, en az 20 karakter içermektedir.")
        viewModel.onPostTypeSelected(PostType.RENTAL)
        viewModel.onCategorySelected(PostCategory.APARTMENT)
        viewModel.onSubmitClick()

        val state = viewModel.uiState.value
        assertNotNull(state.validationError)
        assertNull(state.navigationEvent)
    }

    // ─── UT-POST-003: İçerik çok kısa ────────────────────────────────────────

    @Test
    fun `submit with content too short sets validationError`() = runTest {
        viewModel.onTitleChange("Bu başlık yeterince uzundur kesinlikle")
        viewModel.onContentChange("kısa")   // < 20 karakter
        viewModel.onPostTypeSelected(PostType.RENTAL)
        viewModel.onCategorySelected(PostCategory.APARTMENT)
        viewModel.onSubmitClick()

        val state = viewModel.uiState.value
        assertNotNull(state.validationError)
    }

    // ─── UT-POST-004: Başarılı gönderi oluşturma ─────────────────────────────

    @Test
    fun `successful post creation triggers navigation`() = runTest {
        val title = "Bu başlık yeterince uzundur kesinlikle"
        val content = "Bu içerik de en az 20 karakter içermektedir, yeterince uzun."

        coEvery {
            feedRepository.createPost(
                title = title,
                content = content,
                postType = PostType.RENTAL,
                category = PostCategory.APARTMENT,
                tags = emptyList(),
                imageByteArray = null
            )
        } returns Result.Success("new_post_id_123")

        viewModel.onTitleChange(title)
        viewModel.onContentChange(content)
        viewModel.onPostTypeSelected(PostType.RENTAL)
        viewModel.onCategorySelected(PostCategory.APARTMENT)
        viewModel.onSubmitClick()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.navigationEvent)
    }

    // ─── UT-POST-005: Gönderi oluşturma başarısız ────────────────────────────

    @Test
    fun `failed post creation sets generalError`() = runTest {
        val title = "Bu başlık yeterince uzundur kesinlikle"
        val content = "Bu içerik de en az 20 karakter içermektedir, yeterince uzun."

        coEvery {
            feedRepository.createPost(
                title = title,
                content = content,
                postType = PostType.RENTAL,
                category = PostCategory.APARTMENT,
                tags = emptyList(),
                imageByteArray = null
            )
        } returns Result.Failure(Exception("Sunucu hatası"))

        viewModel.onTitleChange(title)
        viewModel.onContentChange(content)
        viewModel.onPostTypeSelected(PostType.RENTAL)
        viewModel.onCategorySelected(PostCategory.APARTMENT)
        viewModel.onSubmitClick()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.generalError)
        assertNull(state.navigationEvent)
    }

    // ─── UT-POST-006: Etiket ekleme ──────────────────────────────────────────

    @Test
    fun `onTagAdded adds tag to list`() {
        viewModel.onTagAdded("istanbul")

        val state = viewModel.uiState.value
        assertTrue(state.tags.contains("istanbul"))
        assertEquals(1, state.tags.size)
    }

    @Test
    fun `onTagAdded ignores duplicate tags`() {
        viewModel.onTagAdded("istanbul")
        viewModel.onTagAdded("istanbul")

        val state = viewModel.uiState.value
        assertEquals(1, state.tags.size)
    }

    @Test
    fun `onTagAdded respects MAX_TAGS limit`() {
        repeat(6) { index ->
            viewModel.onTagAdded("tag$index")
        }

        val state = viewModel.uiState.value
        assertEquals(CreatePostViewModel.MAX_TAGS, state.tags.size)
    }

    // ─── UT-POST-007: Etiket silme ────────────────────────────────────────────

    @Test
    fun `onTagRemoved removes tag from list`() {
        viewModel.onTagAdded("istanbul")
        viewModel.onTagAdded("ankara")
        viewModel.onTagRemoved("istanbul")

        val state = viewModel.uiState.value
        assertFalse(state.tags.contains("istanbul"))
        assertTrue(state.tags.contains("ankara"))
    }

    // ─── UT-POST-008: Başlık max uzunluk ─────────────────────────────────────

    @Test
    fun `onTitleChange truncates to TITLE_MAX_LENGTH`() {
        val longTitle = "a".repeat(300)
        viewModel.onTitleChange(longTitle)

        val state = viewModel.uiState.value
        assertEquals(CreatePostViewModel.TITLE_MAX_LENGTH, state.title.length)
    }

    // ─── UT-POST-009: İçerik max uzunluk ─────────────────────────────────────

    @Test
    fun `onContentChange truncates to CONTENT_MAX_LENGTH`() {
        val longContent = "a".repeat(6000)
        viewModel.onContentChange(longContent)

        val state = viewModel.uiState.value
        assertEquals(CreatePostViewModel.CONTENT_MAX_LENGTH, state.content.length)
    }

    // ─── UT-POST-010: Görsel kaldırma ────────────────────────────────────────

    @Test
    fun `onImageRemoved clears imageUri`() {
        // Önce URI set et (EXIF temizleme gerektirmeyen basit mock)
        viewModel.onImageSelected(android.net.Uri.EMPTY)
        viewModel.onImageRemoved()

        val state = viewModel.uiState.value
        assertNull(state.imageUri)
    }
}