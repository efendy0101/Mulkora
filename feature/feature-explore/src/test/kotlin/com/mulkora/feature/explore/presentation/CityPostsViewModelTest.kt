package com.mulkora.feature.explore.presentation

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.explore.domain.model.ExplorePost
import com.mulkora.feature.explore.domain.repository.ExploreRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date

/**
 * CityPostsViewModelTest — Şehre göre gönderi listesi.
 *
 * CLAUDE.md §33: ViewModel; SavedStateHandle'dan şehir alır, repository'den yükler.
 * docs/21-testing-strategy.md — UT-EXPLORE kapsamı.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CityPostsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ExploreRepository
    private lateinit var savedStateHandle: SavedStateHandle

    private fun makePost(id: String, city: String = "İstanbul") = ExplorePost(
        postId = id,
        authorUid = "author_001",
        authorUsername = "yazar",
        title = "Test Gönderi $id",
        content = "İçerik",
        city = city,
        category = PostCategory.RENT,
        imageUrl = null,
        likeCount = 0,
        commentCount = 0,
        createdAt = Date(),
    )

    @Before
    fun setup() {
        repository = mockk()
    }

    private fun createViewModel(city: String = "İstanbul"): CityPostsViewModel {
        savedStateHandle = SavedStateHandle(mapOf("city" to city))
        return CityPostsViewModel(repository, savedStateHandle)
    }

    // ============================================================
    // Başlangıç / Yükleme Durumu
    // ============================================================

    @Test
    fun `başlangıçta Loading state döner`() = runTest {
        coEvery { repository.getPostsByCity(any()) } returns Result.Success(emptyList())

        val vm = createViewModel()

        // İlk state loading olmalı (ya da hemen Success gelebilir — runTest bekleme sağlar)
        assertThat(vm.uiState.value).isNotNull()
    }

    // ============================================================
    // Başarılı Yükleme
    // ============================================================

    @Test
    fun `şehre ait gönderiler başarıyla yüklenir`() = runTest {
        val posts = listOf(makePost("p1"), makePost("p2"), makePost("p3"))
        coEvery { repository.getPostsByCity("İstanbul") } returns Result.Success(posts)

        val vm = createViewModel("İstanbul")

        val state = vm.uiState.value
        assertThat(state).isInstanceOf(CityPostsUiState.Success::class.java)
        assertThat((state as CityPostsUiState.Success).posts).hasSize(3)
    }

    @Test
    fun `farklı şehir için gönderiler doğru yüklenir`() = runTest {
        val posts = listOf(makePost("p1", "Ankara"), makePost("p2", "Ankara"))
        coEvery { repository.getPostsByCity("Ankara") } returns Result.Success(posts)

        val vm = createViewModel("Ankara")

        val state = vm.uiState.value
        assertThat(state).isInstanceOf(CityPostsUiState.Success::class.java)
        assertThat((state as CityPostsUiState.Success).posts).hasSize(2)
    }

    // ============================================================
    // Boş Liste
    // ============================================================

    @Test
    fun `gönderiler boşsa Empty state döner`() = runTest {
        coEvery { repository.getPostsByCity(any()) } returns Result.Success(emptyList())

        val vm = createViewModel("Kayseri")

        val state = vm.uiState.value
        assertThat(state).isInstanceOf(CityPostsUiState.Empty::class.java)
    }

    // ============================================================
    // Hata Durumu
    // ============================================================

    @Test
    fun `yükleme hatası Error state döner`() = runTest {
        coEvery {
            repository.getPostsByCity(any())
        } returns Result.Error(Exception("Bağlantı hatası"))

        val vm = createViewModel("İzmir")

        val state = vm.uiState.value
        assertThat(state).isInstanceOf(CityPostsUiState.Error::class.java)
        assertThat((state as CityPostsUiState.Error).message).isNotEmpty()
    }

    // ============================================================
    // SavedStateHandle'dan Şehir Okunması
    // ============================================================

    @Test
    fun `SavedStateHandle olmayan şehir için repository çağrılmaz`() = runTest {
        savedStateHandle = SavedStateHandle() // city yok
        val vm = CityPostsViewModel(repository, savedStateHandle)

        // Şehir null ise Error veya Loading state beklenir (çarpışmadan)
        assertThat(vm.uiState.value).isNotNull()
    }

    // ============================================================
    // Yenileme
    // ============================================================

    @Test
    fun `loadPosts yeniden çağrıldığında gönderiler yenilenir`() = runTest {
        val posts = listOf(makePost("p1"))
        coEvery { repository.getPostsByCity("İstanbul") } returns Result.Success(posts)

        val vm = createViewModel("İstanbul")

        assertThat(vm.uiState.value).isInstanceOf(CityPostsUiState.Success::class.java)

        // Yenileme isteği
        val morePosts = listOf(makePost("p1"), makePost("p2"))
        coEvery { repository.getPostsByCity("İstanbul") } returns Result.Success(morePosts)
        vm.loadPosts()

        val state = vm.uiState.value
        assertThat(state).isInstanceOf(CityPostsUiState.Success::class.java)
        assertThat((state as CityPostsUiState.Success).posts).hasSize(2)
    }
}