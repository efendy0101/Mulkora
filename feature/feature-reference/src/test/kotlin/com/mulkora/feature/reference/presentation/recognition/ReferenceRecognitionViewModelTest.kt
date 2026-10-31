package com.mulkora.feature.reference.presentation.recognition

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
 * ReferenceRecognitionViewModelTest — "Bu kişiyi tanıyor musunuz?" akışı senaryoları.
 *
 * CLAUDE.md §13 Temel Akış:
 *   2. Hedef kullanıcıya "Bu kişiyi tanıyor musunuz?" sorusu gönderilir.
 *   3. "Hayır" cevabında süreç durur ve referans yayımlanmaz.
 *   4. "Evet" cevabında referans hazırlama ekranına geçilir.
 * docs/21-testing-strategy.md — UT-REFERENCE kapsamı.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReferenceRecognitionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ReferenceRepository
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setup() {
        repository = mockk()
    }

    private fun createViewModel(referenceId: String = "ref_001"): ReferenceRecognitionViewModel {
        savedStateHandle = SavedStateHandle(mapOf("referenceId" to referenceId))
        return ReferenceRecognitionViewModel(repository, savedStateHandle)
    }

    // ============================================================
    // Başlangıç Durumu
    // ============================================================

    @Test
    fun `başlangıçta isSuccess false ve errorMessage null`() = runTest {
        val vm = createViewModel()

        val state = vm.uiState.value
        assertThat(state.isSuccess).isFalse()
        assertThat(state.errorMessage).isNull()
        assertThat(state.isLoading).isFalse()
    }

    // ============================================================
    // "Evet" Cevabı
    // ============================================================

    @Test
    fun `respondYes başarıyla gönderilir`() = runTest {
        coEvery {
            repository.respondToRecognitionRequest("ref_001", acknowledged = true)
        } returns Result.Success(Unit)

        val vm = createViewModel("ref_001")
        vm.respondYes()

        val state = vm.uiState.value
        assertThat(state.isSuccess).isTrue()
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `respondYes hata verdiğinde errorMessage ayarlanır`() = runTest {
        coEvery {
            repository.respondToRecognitionRequest(any(), acknowledged = true)
        } returns Result.Error(Exception("Yanıt gönderilemedi"))

        val vm = createViewModel()
        vm.respondYes()

        val state = vm.uiState.value
        assertThat(state.isSuccess).isFalse()
        assertThat(state.errorMessage).isNotNull()
    }

    // ============================================================
    // "Hayır" Cevabı
    // ============================================================

    @Test
    fun `respondNo başarıyla gönderilir ve referans durdurulur`() = runTest {
        coEvery {
            repository.respondToRecognitionRequest("ref_001", acknowledged = false)
        } returns Result.Success(Unit)

        val vm = createViewModel("ref_001")
        vm.respondNo()

        val state = vm.uiState.value
        // "Hayır" yanıtı verildikten sonra süreç durur
        assertThat(state.isSuccess).isTrue()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `respondNo hata verdiğinde errorMessage ayarlanır`() = runTest {
        coEvery {
            repository.respondToRecognitionRequest(any(), acknowledged = false)
        } returns Result.Error(Exception("Yanıt gönderilemedi"))

        val vm = createViewModel()
        vm.respondNo()

        val state = vm.uiState.value
        assertThat(state.isSuccess).isFalse()
        assertThat(state.errorMessage).isNotNull()
    }

    // ============================================================
    // Hata Temizleme
    // ============================================================

    @Test
    fun `clearError errorMessage null olur`() = runTest {
        coEvery {
            repository.respondToRecognitionRequest(any(), acknowledged = true)
        } returns Result.Error(Exception("Hata"))

        val vm = createViewModel()
        vm.respondYes()

        assertThat(vm.uiState.value.errorMessage).isNotNull()
        vm.clearError()
        assertThat(vm.uiState.value.errorMessage).isNull()
    }
}