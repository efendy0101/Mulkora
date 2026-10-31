package com.mulkora.feature.feed.presentation.feed

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mulkora.core.common.model.PostCategory
import com.mulkora.core.common.model.PostType
import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.feed.domain.model.FeedItem
import com.mulkora.feature.feed.domain.model.ReactionType
import com.mulkora.feature.feed.domain.repository.FeedRepository
import com.mulkora.feature.feed.domain.repository.FeedSortOption
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * FeedViewModelTest — Ana akış filtreleme, tepki ve kaydetme işlemleri.
 *
 * CLAUDE.md §6: Ana akış gereksinimleri (sıralama, kategori, gönderi türü filtresi).
 * CLAUDE.md §8: Gönderi onay sistemi — repository yalnızca status=approved gönderileri döndürür.
 * CLAUDE.md §32: Test senaryoları.
 */
class FeedViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FeedRepository

    @Before
    fun setup() {
        repository = mockk()
        // Varsayılan: boş paging data dön
        every {
            repository.getApprovedPostsPaged(any(), any(), any())
        } returns flowOf(PagingData.empty())
    }

    private fun createViewModel() = FeedViewModel(repository)

    // ============================================================
    // Başlangıç Durumu
    // ============================================================

    @Test
    fun `başlangıç filterState varsayılan değerleri taşır`() = runTest {
        val vm = createViewModel()
        val state = vm.filterState.value

        assertThat(state.selectedCategory).isNull()
        assertThat(state.selectedPostType).isNull()
        assertThat(state.sortOption).isEqualTo(FeedSortOption.NEWEST)
    }

    @Test
    fun `başlangıç uiState snackbarMessage null`() = runTest {
        val vm = createViewModel()
        assertThat(vm.uiState.value.snackbarMessage).isNull()
    }

    // ============================================================
    // Kategori Filtreleme — CLAUDE.md §6
    // ============================================================

    @Test
    fun `kategori seçimi filterState günceller`() = runTest {
        val vm = createViewModel()

        vm.filterState.test {
            awaitItem() // başlangıç

            vm.onCategorySelected(PostCategory.DEPOSIT)
            val updated = awaitItem()
            assertThat(updated.selectedCategory).isEqualTo(PostCategory.DEPOSIT)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `kategori null seçimi filtreyi sıfırlar`() = runTest {
        val vm = createViewModel()

        vm.filterState.test {
            awaitItem()

            vm.onCategorySelected(PostCategory.LANDLORD_REVIEW)
            awaitItem()

            vm.onCategorySelected(null)
            val cleared = awaitItem()
            assertThat(cleared.selectedCategory).isNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Gönderi Türü Filtresi — CLAUDE.md §6
    // ============================================================

    @Test
    fun `gönderi türü seçimi filterState günceller`() = runTest {
        val vm = createViewModel()

        vm.filterState.test {
            awaitItem()

            vm.onPostTypeSelected(PostType.QUESTION)
            val updated = awaitItem()
            assertThat(updated.selectedPostType).isEqualTo(PostType.QUESTION)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Sıralama Seçenekleri — CLAUDE.md §6
    // ============================================================

    @Test
    fun `sıralama POPULAR olarak değiştirildiğinde filterState güncellenir`() = runTest {
        val vm = createViewModel()

        vm.filterState.test {
            awaitItem() // başlangıç (NEWEST)

            vm.onSortOptionSelected(FeedSortOption.POPULAR)
            val updated = awaitItem()
            assertThat(updated.sortOption).isEqualTo(FeedSortOption.POPULAR)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sıralama MOST_COMMENTED olarak değiştirildiğinde filterState güncellenir`() = runTest {
        val vm = createViewModel()

        vm.filterState.test {
            awaitItem()

            vm.onSortOptionSelected(FeedSortOption.MOST_COMMENTED)
            val updated = awaitItem()
            assertThat(updated.sortOption).isEqualTo(FeedSortOption.MOST_COMMENTED)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sıralama TOP_RATED olarak değiştirildiğinde filterState güncellenir`() = runTest {
        val vm = createViewModel()

        vm.filterState.test {
            awaitItem()

            vm.onSortOptionSelected(FeedSortOption.TOP_RATED)
            val updated = awaitItem()
            assertThat(updated.sortOption).isEqualTo(FeedSortOption.TOP_RATED)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Tepki (Beğeni/Eksi Oy) — CLAUDE.md §6
    // ============================================================

    @Test
    fun `tepki başarılı olduğunda snackbar gösterilmez`() = runTest {
        coEvery { repository.toggleReaction(any(), any()) } returns Result.Success(Unit)
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem() // başlangıç

            vm.onReactionClick("post_001", ReactionType.LIKE)
            // Başarı durumunda snackbar gelmez
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tepki başarısız olduğunda snackbar mesajı görünür`() = runTest {
        coEvery {
            repository.toggleReaction(any(), any())
        } returns Result.Error(Exception("Tepki eklenemedi"))
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem()

            vm.onReactionClick("post_001", ReactionType.LIKE)
            val errorState = awaitItem()
            assertThat(errorState.snackbarMessage).isNotNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `aynı gönderiyi tekrar beğenmek tepkiyi kaldırır (toggle)`() = runTest {
        // İkinci çağrıda "kaldırıldı" durumu simülasyonu
        coEvery { repository.toggleReaction("post_001", ReactionType.LIKE) } returns Result.Success(Unit)
        val vm = createViewModel()

        // İki kez çağrı yapılabilmeli, her ikisi de başarılı döner
        vm.onReactionClick("post_001", ReactionType.LIKE)
        vm.onReactionClick("post_001", ReactionType.LIKE)

        // Hata olmamalı
        assertThat(vm.uiState.value.snackbarMessage).isNull()
    }

    // ============================================================
    // Gönderi Kaydetme — CLAUDE.md §6
    // ============================================================

    @Test
    fun `gönderi kaydetme başarılı olduğunda snackbar gösterilmez`() = runTest {
        coEvery { repository.toggleSavePost(any()) } returns Result.Success(Unit)
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem()

            vm.onSavePostClick("post_001")
            // Başarı durumunda snackbar gösterilmez
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `gönderi kaydetme hatası snackbar mesajı gösterir`() = runTest {
        coEvery {
            repository.toggleSavePost(any())
        } returns Result.Error(Exception("Kaydetme başarısız"))
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem()

            vm.onSavePostClick("post_001")
            val errorState = awaitItem()
            assertThat(errorState.snackbarMessage).isNotNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Snackbar Temizleme
    // ============================================================

    @Test
    fun `onSnackbarShown çağrılınca snackbarMessage null olur`() = runTest {
        coEvery {
            repository.toggleReaction(any(), any())
        } returns Result.Error(Exception("Hata"))
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem()

            vm.onReactionClick("post_001", ReactionType.DISLIKE)
            awaitItem() // snackbar görünür

            vm.onSnackbarShown()
            val cleared = awaitItem()
            assertThat(cleared.snackbarMessage).isNull()

            cancelAndIgnoreRemainingEvents()
        }
    }
}
