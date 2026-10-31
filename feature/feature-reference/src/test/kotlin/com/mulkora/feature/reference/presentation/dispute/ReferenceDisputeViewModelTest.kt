package com.mulkora.feature.reference.presentation.dispute

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.reference.domain.repository.ReferenceRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * ReferenceDisputeViewModelTest — Referans itiraz ekranı senaryoları.
 *
 * CLAUDE.md §13 Temel Akış:
 *   11. Hedef kullanıcı referansa itiraz edebilir.
 *   12. İtiraz yönetici paneline düşer.
 * docs/21-testing-strategy.md — UT-REFERENCE kapsamı.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReferenceDisputeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ReferenceRepository
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setup() {
        repository = mockk()
    }

    private fun createViewModel(referenceId: String = "ref_001"): ReferenceDisputeViewModel {
        savedStateHandle = SavedStateHandle(mapOf("referenceId" to referenceId))
        return ReferenceDisputeViewModel(repository, savedStateHandle)
    }

    // ============================================================
    // Başlangıç Durumu
    // ============================================================

    @Test
    fun `başlangıçta tüm alanlar varsayılan değerlerde`() {
        val vm = createViewModel()
        val state = vm.uiState.value

        assertThat(state.isLoading).isFalse()
        assertThat(state.isSubmitted).isFalse()
        assertThat(state.errorMessage).isNull()
        assertThat(state.reason).isEmpty()
        assertThat(state.explanation).isEmpty()
    }

    // ============================================================
    // Form Girişleri
    // ============================================================

    @Test
    fun `onReasonChanged itiraz nedenini günceller`() {
        val vm = createViewModel()
        vm.onReasonChanged("Yanlış bilgi içeriyor")
        assertThat(vm.uiState.value.reason).isEqualTo("Yanlış bilgi içeriyor")
    }

    @Test
    fun `onExplanationChanged açıklamayı günceller`() {
        val vm = createViewModel()
        vm.onExplanationChanged("Bu referans tamamen asılsız iddialara dayanmaktadır.")
        assertThat(vm.uiState.value.explanation)
            .isEqualTo("Bu referans tamamen asılsız iddialara dayanmaktadır.")
    }

    // ============================================================
    // İtiraz Gönderme — Başarı
    // ============================================================

    @Test
    fun `submitDispute başarıyla gönderilir`() = runTest {
        coEvery {
            repository.submitDispute(referenceId = "ref_001", reason = any(), explanation = any())
        } returns Result.Success(Unit)

        val vm = createViewModel("ref_001")
        vm.onReasonChanged("Yanlış bilgi")
        vm.onExplanationChanged("Detaylı açıklama.")
        vm.submitDispute()

        val state = vm.uiState.value
        assertThat(state.isSubmitted).isTrue()
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    // ============================================================
    // İtiraz Gönderme — Doğrulama Hataları
    // ============================================================

    @Test
    fun `submitDispute neden alanı boşsa hata verir`() = runTest {
        val vm = createViewModel()
        vm.onExplanationChanged("Açıklama var ama neden yok.")
        vm.submitDispute()

        val state = vm.uiState.value
        assertThat(state.isSubmitted).isFalse()
        assertThat(state.errorMessage).isNotNull()
    }

    @Test
    fun `submitDispute açıklama alanı boşsa hata verir`() = runTest {
        val vm = createViewModel()
        vm.onReasonChanged("Yanlış bilgi")
        // Açıklama boş
        vm.submitDispute()

        val state = vm.uiState.value
        assertThat(state.isSubmitted).isFalse()
        assertThat(state.errorMessage).isNotNull()
    }

    @Test
    fun `submitDispute her iki alan da boşsa hata verir`() = runTest {
        val vm = createViewModel()
        vm.submitDispute()

        assertThat(vm.uiState.value.isSubmitted).isFalse()
        assertThat(vm.uiState.value.errorMessage).isNotNull()
    }

    // ============================================================
    // İtiraz Gönderme — Repository Hatası
    // ============================================================

    @Test
    fun `submitDispute repository hatası errorMessage ayarlar`() = runTest {
        coEvery {
            repository.submitDispute(referenceId = any(), reason = any(), explanation = any())
        } returns Result.Error(Exception("Sunucu hatası"))

        val vm = createViewModel()
        vm.onReasonChanged("Yanıltıcı içerik")
        vm.onExplanationChanged("Bu bilgiler gerçeği yansıtmıyor.")
        vm.submitDispute()

        val state = vm.uiState.value
        assertThat(state.isSubmitted).isFalse()
        assertThat(state.errorMessage).isNotNull()
        assertThat(state.isLoading).isFalse()
    }

    // ============================================================
    // Hata Temizleme
    // ============================================================

    @Test
    fun `clearError errorMessage null olur`() = runTest {
        coEvery {
            repository.submitDispute(referenceId = any(), reason = any(), explanation = any())
        } returns Result.Error(Exception("Hata"))

        val vm = createViewModel()
        vm.onReasonChanged("Neden")
        vm.onExplanationChanged("Açıklama.")
        vm.submitDispute()

        assertThat(vm.uiState.value.errorMessage).isNotNull()
        vm.clearError()
        assertThat(vm.uiState.value.errorMessage).isNull()
    }
}