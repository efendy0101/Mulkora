package com.mulkora.feature.reference.presentation.write

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
 * ReferenceWriteViewModelTest — Referans yazma ekranı senaryoları.
 *
 * CLAUDE.md §13: "Evet" cevabı sonrasında referans yazma ekranı açılır.
 *   - Kiracı değerlendirme alanları: kira ödeme düzeni, evi kullanım, iletişim vb.
 *   - Ev sahibi değerlendirme alanları: iletişim, bakım, depozito, sözleşme uyumu vb.
 *   - Her alan için: 1-5 puan, olumlu/tarafsız/olumsuz, açıklama.
 * docs/21-testing-strategy.md — UT-REFERENCE kapsamı.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReferenceWriteViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ReferenceRepository
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setup() {
        repository = mockk()
    }

    private fun createViewModel(referenceId: String = "ref_001"): ReferenceWriteViewModel {
        savedStateHandle = SavedStateHandle(mapOf("referenceId" to referenceId))
        return ReferenceWriteViewModel(repository, savedStateHandle)
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
        assertThat(state.overallRating).isNull()
        assertThat(state.comment).isEmpty()
    }

    // ============================================================
    // Puan ve Yorum Girişleri
    // ============================================================

    @Test
    fun `onOverallRatingChanged puanı günceller`() {
        val vm = createViewModel()
        vm.onOverallRatingChanged(4.0f)
        assertThat(vm.uiState.value.overallRating).isEqualTo(4.0f)
    }

    @Test
    fun `onCommentChanged yorumu günceller`() {
        val vm = createViewModel()
        vm.onCommentChanged("Çok titiz ve ödeme konusunda güvenilirdi.")
        assertThat(vm.uiState.value.comment).isEqualTo("Çok titiz ve ödeme konusunda güvenilirdi.")
    }

    @Test
    fun `onSentimentChanged genel değerlendirmeyi günceller`() {
        val vm = createViewModel()
        vm.onSentimentChanged(ReferenceSentiment.POSITIVE)
        assertThat(vm.uiState.value.sentiment).isEqualTo(ReferenceSentiment.POSITIVE)
    }

    // ============================================================
    // Alt Alan Puanlamaları
    // ============================================================

    @Test
    fun `onFieldRatingChanged alan puanını günceller`() {
        val vm = createViewModel()
        vm.onFieldRatingChanged(ReferenceField.COMMUNICATION, 5.0f)
        assertThat(vm.uiState.value.fieldRatings[ReferenceField.COMMUNICATION]).isEqualTo(5.0f)
    }

    @Test
    fun `birden fazla alan puanı bağımsız güncellenir`() {
        val vm = createViewModel()
        vm.onFieldRatingChanged(ReferenceField.COMMUNICATION, 3.0f)
        vm.onFieldRatingChanged(ReferenceField.PAYMENT, 5.0f)

        assertThat(vm.uiState.value.fieldRatings[ReferenceField.COMMUNICATION]).isEqualTo(3.0f)
        assertThat(vm.uiState.value.fieldRatings[ReferenceField.PAYMENT]).isEqualTo(5.0f)
    }

    // ============================================================
    // Referans Gönderme
    // ============================================================

    @Test
    fun `submitReference başarıyla gönderilir`() = runTest {
        coEvery {
            repository.submitWrittenReference(referenceId = "ref_001", data = any())
        } returns Result.Success(Unit)

        val vm = createViewModel("ref_001")
        vm.onOverallRatingChanged(4.5f)
        vm.onCommentChanged("Harika bir kiracıydı.")
        vm.onSentimentChanged(ReferenceSentiment.POSITIVE)
        vm.submitReference()

        val state = vm.uiState.value
        assertThat(state.isSubmitted).isTrue()
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `submitReference puan eksikse hata verir`() = runTest {
        val vm = createViewModel()
        vm.onCommentChanged("Bir yorum.")
        vm.submitReference()

        assertThat(vm.uiState.value.isSubmitted).isFalse()
        assertThat(vm.uiState.value.errorMessage).isNotNull()
    }

    @Test
    fun `submitReference yorum eksikse hata verir`() = runTest {
        val vm = createViewModel()
        vm.onOverallRatingChanged(3.0f)
        // Yorum boş bırakıldı
        vm.submitReference()

        assertThat(vm.uiState.value.isSubmitted).isFalse()
        assertThat(vm.uiState.value.errorMessage).isNotNull()
    }

    @Test
    fun `submitReference repository hatası errorMessage ayarlar`() = runTest {
        coEvery {
            repository.submitWrittenReference(referenceId = any(), data = any())
        } returns Result.Error(Exception("Sunucu hatası"))

        val vm = createViewModel()
        vm.onOverallRatingChanged(4.0f)
        vm.onCommentChanged("İyi bir deneyimdi.")
        vm.onSentimentChanged(ReferenceSentiment.NEUTRAL)
        vm.submitReference()

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
            repository.submitWrittenReference(referenceId = any(), data = any())
        } returns Result.Error(Exception("Hata"))

        val vm = createViewModel()
        vm.onOverallRatingChanged(3.0f)
        vm.onCommentChanged("Yorum.")
        vm.onSentimentChanged(ReferenceSentiment.NEGATIVE)
        vm.submitReference()

        assertThat(vm.uiState.value.errorMessage).isNotNull()
        vm.clearError()
        assertThat(vm.uiState.value.errorMessage).isNull()
    }
}