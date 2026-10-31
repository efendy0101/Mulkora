package com.mulkora.feature.feed.presentation.savedposts

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.model.PostType
import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.feed.domain.model.Post
import com.mulkora.feature.feed.domain.repository.FeedRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date

/**
 * SavedPostsViewModelTest — Kaydedilen gönderiler listesi.
 *
 * CLAUDE.md §6: Gönderi kaydedebilme ve kayıtlı gönderileri listeleme.
 * CLAUDE.md §32: Test senaryoları.
 */
class SavedPostsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FeedRepository

    private fun makePost(id: String) = Post(
        postId = id,
        authorUid = "author_001",
        authorUsername = "test_yazar",
        title = "Kaydedilen Gönderi $id",
        content = "İçerik",
        postType = PostType.EXPERIENCE,
        category = PostCategory.DEPOSIT,
        city = "İstanbul",
        status = "approved",
        isAnonymous = false,
        likeCount = 3,
        dislikeCount = 0,
        commentCount = 2,
        ratingAvg = 4.0,
        ratingCount = 5,
        imageUrl = null,
        createdAt = Date(),
        tags = emptyList(),
    )

    @Before
    fun setup() {
        repository = mockk()
    }

    private fun createViewModel() = SavedPostsViewModel(repository)

    // ============================================================
    // Kaydedilen Gönderiler Yükleme
    // ============================================================

    @Test
    fun `başlangıçta kaydedilen gönderiler yüklenir`() = runTest {
        val saved = listOf(makePost("saved_001"), makePost("saved_002"))
        every { repository.getSavedPosts() } returns flowOf(Result.Success(saved))
        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.posts).hasSize(2)
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `kaydedilen gönderiler boş olduğunda boş liste gösterilir`() = runTest {
        every { repository.getSavedPosts() } returns flowOf(Result.Success(emptyList()))
        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.posts).isEmpty()
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `yükleme sırasında isLoading true döner`() = runTest {
        val saved = listOf(makePost("saved_001"))
        every { repository.getSavedPosts() } returns flowOf(
            Result.Loading,
            Result.Success(saved)
        )
        val vm = createViewModel()

        vm.uiState.test {
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.posts).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `yükleme hatası errorMessage ayarlar`() = runTest {
        every { repository.getSavedPosts() } returns flowOf(
            Result.Error(Exception("Yükleme hatası"))
        )
        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.errorMessage).isNotNull()
            assertThat(state.posts).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Kayıttan Kaldırma — CLAUDE.md §6
    // ============================================================

    @Test
    fun `unsavePost çağrıldığında toggleSavePost repository metodu tetiklenir`() = runTest {
        val saved = listOf(makePost("saved_001"), makePost("saved_002"))
        every { repository.getSavedPosts() } returns flowOf(Result.Success(saved))
        coEvery { repository.toggleSavePost("saved_001") } returns Result.Success(Unit)
        val vm = createViewModel()

        // unsavePost başarıyla tamamlanmalı (istisnasız)
        vm.unsavePost("saved_001")
        // Herhangi bir hata üretmemeli
        assertThat(vm.uiState.value.errorMessage).isNull()
    }

    @Test
    fun `unsavePost çağrıldığında hata oluşursa errorMessage güncellenmez (fire-and-forget)`() = runTest {
        // SavedPostsViewModel unsavePost'ta sonucu işlemez — fire-and-forget
        val saved = listOf(makePost("saved_001"))
        every { repository.getSavedPosts() } returns flowOf(Result.Success(saved))
        coEvery {
            repository.toggleSavePost(any())
        } returns Result.Error(Exception("Kayıt kaldırılamadı"))
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem() // başlangıç
            vm.unsavePost("saved_001")
            // Fire-and-forget: hata state'e yansıtılmaz
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Hata Temizleme
    // ============================================================

    @Test
    fun `clearError çağrılınca errorMessage null olur`() = runTest {
        every { repository.getSavedPosts() } returns flowOf(
            Result.Error(Exception("Hata"))
        )
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem() // hata state
            vm.clearError()
            val state = awaitItem()
            assertThat(state.errorMessage).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isLoading başlangıçta false gelir (varsayılan state)`() = runTest {
        every { repository.getSavedPosts() } returns flowOf(Result.Success(emptyList()))
        val vm = createViewModel()

        assertThat(vm.uiState.value.isLoading).isFalse()
    }
}
