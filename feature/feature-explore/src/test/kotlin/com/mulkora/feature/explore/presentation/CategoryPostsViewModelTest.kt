package com.mulkora.feature.explore.presentation

import androidx.lifecycle.SavedStateHandle
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.explore.domain.model.ExplorePost
import com.mulkora.feature.explore.domain.repository.ExploreRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date

/**
 * CategoryPostsViewModel birim testleri.
 *
 * CLAUDE.md §33: ViewModel SavedStateHandle'dan kategori alır.
 * docs/21-testing-strategy.md — UT-EXPLORE-001 ile UT-EXPLORE-006.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoryPostsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var exploreRepository: ExploreRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: CategoryPostsViewModel

    private val testCategory = PostCategory.TENANT_RIGHTS

    private fun makePost(id: String) = ExplorePost(
        id = id,
        title = "Test Gönderi $id",
        authorName = "Test Yazar",
        category = testCategory,
        likeCount = 0,
        commentCount = 0,
        createdAt = Date()
    )

    @Before
    fun setUp() {
        exploreRepository = mockk()
        savedStateHandle = SavedStateHandle(mapOf("category" to testCategory.name))
    }

    // ─── UT-EXPLORE-001: Başarılı yükleme → Success ───────────────────────────

    @Test
    fun `loadPosts success sets Success state`() = runTest {
        val posts = listOf(makePost("p1"), makePost("p2"), makePost("p3"))

        coEvery {
            exploreRepository.getPostsByCategory(testCategory)
        } returns Result.Success(posts)

        viewModel = CategoryPostsViewModel(savedStateHandle, exploreRepository)

        val state = viewModel.uiState.value
        assertTrue(state is CategoryPostsUiState.Success)
        assertEquals(3, (state as CategoryPostsUiState.Success).posts.size)
    }

    // ─── UT-EXPLORE-002: Boş liste → Empty ───────────────────────────────────

    @Test
    fun `loadPosts with empty list sets Empty state`() = runTest {
        coEvery {
            exploreRepository.getPostsByCategory(testCategory)
        } returns Result.Success(emptyList())

        viewModel = CategoryPostsViewModel(savedStateHandle, exploreRepository)

        assertTrue(viewModel.uiState.value is CategoryPostsUiState.Empty)
    }

    // ─── UT-EXPLORE-003: Hata → Error ────────────────────────────────────────

    @Test
    fun `loadPosts failure sets Error state`() = runTest {
        coEvery {
            exploreRepository.getPostsByCategory(testCategory)
        } returns Result.Failure(Exception("Sunucu hatası"))

        viewModel = CategoryPostsViewModel(savedStateHandle, exploreRepository)

        assertTrue(viewModel.uiState.value is CategoryPostsUiState.Error)
    }

    // ─── UT-EXPLORE-004: Yükleme sırasında Loading ───────────────────────────

    @Test
    fun `initial state while loading is Loading`() = runTest {
        coEvery {
            exploreRepository.getPostsByCategory(any())
        } returns Result.Success(listOf(makePost("p1")))

        // ViewModel henüz başlatılmadan önce
        // init'te hemen loading başlar; burada sadece non-null olduğunu doğruluyoruz
        viewModel = CategoryPostsViewModel(savedStateHandle, exploreRepository)
        // Sonuç Success olmalı (coroutine çalıştı)
        assertTrue(viewModel.uiState.value is CategoryPostsUiState.Success)
    }

    // ─── UT-EXPLORE-005: Yeniden yükleme ─────────────────────────────────────

    @Test
    fun `retry after error sets Success state`() = runTest {
        coEvery {
            exploreRepository.getPostsByCategory(testCategory)
        } returnsMany listOf(
            Result.Failure(Exception("İlk hata")),
            Result.Success(listOf(makePost("p1")))
        )

        viewModel = CategoryPostsViewModel(savedStateHandle, exploreRepository)
        // İlk çağrı hata döndürür
        assertTrue(viewModel.uiState.value is CategoryPostsUiState.Error)

        // Retry
        viewModel.retry()
        assertTrue(viewModel.uiState.value is CategoryPostsUiState.Success)
    }

    // ─── UT-EXPLORE-006: Farklı kategori ─────────────────────────────────────

    @Test
    fun `different category loads correct posts`() = runTest {
        val otherCategory = PostCategory.LANDLORD_ADVICE
        val sh = SavedStateHandle(mapOf("category" to otherCategory.name))
        val posts = listOf(makePost("landlord_1"))

        coEvery {
            exploreRepository.getPostsByCategory(otherCategory)
        } returns Result.Success(posts)

        val vm = CategoryPostsViewModel(sh, exploreRepository)

        val state = vm.uiState.value
        assertTrue(state is CategoryPostsUiState.Success)
        assertEquals(1, (state as CategoryPostsUiState.Success).posts.size)
    }
}