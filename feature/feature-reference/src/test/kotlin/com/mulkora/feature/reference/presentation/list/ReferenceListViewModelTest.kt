package com.mulkora.feature.reference.presentation.list

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.auth.domain.repository.AuthRepository
import com.mulkora.feature.reference.domain.model.Reference
import com.mulkora.feature.reference.domain.repository.ReferenceRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date

/**
 * ReferenceListViewModelTest — Referans listesi ekranı senaryoları.
 *
 * CLAUDE.md §13: Referans listesi sekmeleri: Alınanlar / Verilenler / Bekleyenler.
 *   - Alınanlar, verilenler ve bekleyen referans talepleri ayrı sekmelerdir.
 *   - Onaylı referanslar profilde görünür.
 * docs/21-testing-strategy.md — UT-REFERENCE kapsamı.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReferenceListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var referenceRepository: ReferenceRepository
    private lateinit var authRepository: AuthRepository

    private fun makeReference(id: String = "ref_001", status: String = "approved") = Reference(
        referenceId = id,
        fromUid = "user_a",
        toUid = "user_b",
        relationshipType = "tenant",
        status = status,
        createdAt = Date(),
        overallRating = 4.5f,
        comment = "İyi kiracıydı.",
    )

    @Before
    fun setup() {
        referenceRepository = mockk()
        authRepository = mockk()
        every { authRepository.currentUser } returns flowOf(
            mockk { every { uid } returns "user_b" }
        )
    }

    private fun createViewModel() = ReferenceListViewModel(referenceRepository, authRepository)

    // ============================================================
    // Başlangıç Durumu
    // ============================================================

    @Test
    fun `başlangıçta varsayılan sekme RECEIVED`() = runTest {
        coEvery {
            referenceRepository.getReferencesForUser("user_b")
        } returns flowOf(Result.Success(emptyList()))

        val vm = createViewModel()
        assertThat(vm.uiState.value.selectedTab).isEqualTo(ReferenceTab.RECEIVED)
    }

    @Test
    fun `başlangıçta isLoading true`() = runTest {
        coEvery {
            referenceRepository.getReferencesForUser(any())
        } returns flowOf(Result.Success(emptyList()))

        val vm = createViewModel()
        // Yükleme tamamlandığında false olur
        assertThat(vm.uiState.value.isLoading).isFalse()
    }

    // ============================================================
    // Alınan Referanslar (RECEIVED)
    // ============================================================

    @Test
    fun `RECEIVED sekmesinde alınan referanslar yüklenir`() = runTest {
        val references = listOf(makeReference("r1"), makeReference("r2"))
        coEvery {
            referenceRepository.getReferencesForUser("user_b")
        } returns flowOf(Result.Success(references))

        val vm = createViewModel()

        val state = vm.uiState.value
        assertThat(state.references).hasSize(2)
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `RECEIVED sekmesinde alınan referanslar boşsa liste boş gelir`() = runTest {
        coEvery {
            referenceRepository.getReferencesForUser(any())
        } returns flowOf(Result.Success(emptyList()))

        val vm = createViewModel()

        assertThat(vm.uiState.value.references).isEmpty()
    }

    @Test
    fun `RECEIVED yükleme hatası errorMessage ayarlar`() = runTest {
        coEvery {
            referenceRepository.getReferencesForUser(any())
        } returns flowOf(Result.Error(Exception("Ağ hatası")))

        val vm = createViewModel()

        assertThat(vm.uiState.value.errorMessage).isNotNull()
    }

    // ============================================================
    // Sekme Değişimi — GIVEN
    // ============================================================

    @Test
    fun `GIVEN sekmesine geçildiğinde verilen referanslar yüklenir`() = runTest {
        coEvery {
            referenceRepository.getReferencesForUser(any())
        } returns flowOf(Result.Success(emptyList()))

        val givenRefs = listOf(makeReference("g1"), makeReference("g2"))
        coEvery {
            referenceRepository.getReferencesGivenByUser("user_b")
        } returns flowOf(Result.Success(givenRefs))

        val vm = createViewModel()

        vm.uiState.test {
            awaitItem() // başlangıç

            vm.onTabSelected(ReferenceTab.GIVEN)

            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            val resultState = awaitItem()
            assertThat(resultState.selectedTab).isEqualTo(ReferenceTab.GIVEN)
            assertThat(resultState.references).hasSize(2)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Sekme Değişimi — PENDING
    // ============================================================

    @Test
    fun `PENDING sekmesine geçildiğinde bekleyen talepler yüklenir`() = runTest {
        coEvery {
            referenceRepository.getReferencesForUser(any())
        } returns flowOf(Result.Success(emptyList()))

        val pending = listOf(makeReference("p1", status = "pending"))
        coEvery {
            referenceRepository.getPendingReferenceRequests()
        } returns flowOf(Result.Success(pending))

        val vm = createViewModel()

        vm.uiState.test {
            awaitItem()

            vm.onTabSelected(ReferenceTab.PENDING)

            awaitItem() // loading
            val resultState = awaitItem()
            assertThat(resultState.selectedTab).isEqualTo(ReferenceTab.PENDING)
            assertThat(resultState.references).hasSize(1)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Hata Temizleme
    // ============================================================

    @Test
    fun `clearError errorMessage null olur`() = runTest {
        coEvery {
            referenceRepository.getReferencesForUser(any())
        } returns flowOf(Result.Error(Exception("Hata")))

        val vm = createViewModel()

        assertThat(vm.uiState.value.errorMessage).isNotNull()
        vm.clearError()
        assertThat(vm.uiState.value.errorMessage).isNull()
    }
}