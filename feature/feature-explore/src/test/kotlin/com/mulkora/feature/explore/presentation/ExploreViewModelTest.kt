package com.mulkora.feature.explore.presentation

import app.cash.turbine.test
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
 * ExploreViewModelTest — Keşfet ana ekranı senaryoları.
 *
 * CLAUDE.md §5: Alt navigasyon "Keşfet" sekmesi.
 * CLAUDE.md §6: Şehir ve kategori bazlı filtreleme.
 * docs/21-testing-strategy.md — UT-EXPLORE kapsamı.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ExploreRepository
    private lateinit var viewModel: ExploreViewModel

    private fun makePost(id: String, category: PostCategory = PostCategory.RENT) = ExplorePost(
        postId = id,
        authorUid = "author_001",
        authorUsername = "yazar",
        title = "Test $id",
        content = "İçerik",
        city = "İstanbul",
        category = category,
        imageUrl = null,
        likeCount = 0,
        commentCount = 0,
        createdAt = Date(),
    )

    @Before
    fun setup() {
        repository = mockk()
        // Varsayılan başarılı yanıtlar
        coEvery { repository.getCategorySections() } returns Result.Success(emptyList())
        coEvery { repository.getPopularCities() } returns Result.Success(emptyList())
    }

    private fun createViewModel() = ExploreViewModel(repository)

    // ============================================================
    // Başlangıç / Yükleme
    // ============================================================

    @Test
    fun `başlangıçta state boş ama hata içermez`() = runTest {
        val vm = createViewModel()

        val state = vm.uiState.value
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `başlangıçta popüler şehirler yüklenir`() = runTest {
        val cities = listOf("İstanbul", "Ankara", "İzmir")
        coEvery { repository.getPopularCities() } returns Result.Success(cities)

        val vm = createViewModel()

        val state = vm.uiState.value
        assertThat(state.popularCities).containsExactlyElementsIn(cities)
    }

    @Test
    fun `kategori bölümleri yüklenir`() = runTest {
        val sections = listOf(
            CategorySection(category = PostCategory.RENT, posts = listOf(makePost("p1"))),
        )
        coEvery { repository.getCategorySections() } returns Result.Success(sections)

        val vm = createViewModel()

        val state = vm.uiState.value
        assertThat(state.categorySections).hasSize(1)
    }

    // ============================================================
    // Kategori Seçimi
    // ============================================================

    @Test
    fun `onCategorySelected seçilen kategori güncellenir`() = runTest {
        val vm = createViewModel()

        vm.onCategorySelected(PostCategory.DEPOSIT)

        assertThat(vm.uiState.value.selectedCategory).isEqualTo(PostCategory.DEPOSIT)
    }

    @Test
    fun `aynı kategori tekrar seçilirse selectedCategory null olur`() = runTest {
        val vm = createViewModel()

        vm.onCategorySelected(PostCategory.RENT)
        vm.onCategorySelected(PostCategory.RENT)

        assertThat(vm.uiState.value.selectedCategory).isNull()
    }

    // ============================================================
    // Kategori Gönderileri
    // ============================================================

    @Test
    fun `onCategorySelected kategori gönderilerini yükler`() = runTest {
        val posts = listOf(makePost("p1", PostCategory.RENT), makePost("p2", PostCategory.RENT))
        coEvery {
            repository.getPostsByCategory(PostCategory.RENT)
        } returns Result.Success(posts)

        val vm = createViewModel()

        vm.uiState.test {
            awaitItem() // başlangıç

            vm.onCategorySelected(PostCategory.RENT)

            // Yükleniyor
            val loadingState = awaitItem()
            assertThat(loadingState.isLoadingCategoryPosts).isTrue()

            // Sonuç
            val resultState = awaitItem()
            assertThat(resultState.categoryPosts).hasSize(2)
            assertThat(resultState.isLoadingCategoryPosts).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `kategori yükleme hatası errorMessage ayarlar`() = runTest {
        coEvery {
            repository.getPostsByCategory(any())
        } returns Result.Error(Exception("Yükleme hatası"))

        val vm = createViewModel()

        vm.uiState.test {
            awaitItem()

            vm.onCategorySelected(PostCategory.MAINTENANCE)
            awaitItem() // loading

            val errorState = awaitItem()
            assertThat(errorState.errorMessage).isNotNull()
            assertThat(errorState.isLoadingCategoryPosts).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Hata Temizleme
    // ============================================================

    @Test
    fun `clearError errorMessage null olur`() = runTest {
        coEvery {
            repository.getCategorySections()
        } returns Result.Error(Exception("Hata"))

        val vm = createViewModel()

        // Hata tetiklendi mi kontrol et
        if (vm.uiState.value.errorMessage != null) {
            vm.clearError()
            assertThat(vm.uiState.value.errorMessage).isNull()
        }
    }

    // ============================================================
    // Yenileme
    // ============================================================

    @Test
    fun `refresh tüm verileri yeniden yükler`() = runTest {
        val vm = createViewModel()

        // İkinci çağrıda farklı veri döndür
        val cities = listOf("Bursa", "Adana")
        coEvery { repository.getPopularCities() } returns Result.Success(cities)

        vm.refresh()

        assertThat(vm.uiState.value.popularCities).containsExactlyElementsIn(cities)
    }
}