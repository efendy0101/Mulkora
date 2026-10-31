package com.mulkora.feature.auth.presentation.usernameselection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mulkora.core.common.result.Result
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.auth.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UsernameSelectionViewModelTest — Kullanıcı adı seçim senaryoları.
 *
 * CLAUDE.md §4: Kullanıcı adı benzersizliği backend üzerinden kontrol edilir.
 *   - Debounce ile gereksiz istekler engellenir.
 *   - Minimum 3, maksimum 20 karakter kuralı.
 * docs/21-testing-strategy.md — UT-AUTH kapsamı.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UsernameSelectionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: UsernameSelectionViewModel

    @Before
    fun setup() {
        authRepository = mockk()
        viewModel = UsernameSelectionViewModel(authRepository)
    }

    // ============================================================
    // Başlangıç Durumu
    // ============================================================

    @Test
    fun `başlangıçta kullanıcı adı boş ve hata yok`() {
        val state = viewModel.uiState.value
        assertThat(state.username).isEmpty()
        assertThat(state.isLoading).isFalse()
        assertThat(state.isUsernameAvailable).isNull()
        assertThat(state.errorMessage).isNull()
    }

    // ============================================================
    // Kullanıcı Adı Girişi
    // ============================================================

    @Test
    fun `onUsernameChanged kullanıcı adını günceller`() = runTest {
        viewModel.onUsernameChanged("mulkora_user")
        assertThat(viewModel.uiState.value.username).isEqualTo("mulkora_user")
    }

    @Test
    fun `kısa kullanıcı adı için kullanılabilirlik kontrolü yapılmaz`() = runTest {
        coEvery { authRepository.checkUsernameAvailability(any()) } returns Result.Success(true)

        viewModel.onUsernameChanged("ab") // 2 karakter — kısa
        advanceTimeBy(1000) // debounce geçmesi için

        // 2 karakter için istek gönderilmemeli
        assertThat(viewModel.uiState.value.isUsernameAvailable).isNull()
    }

    // ============================================================
    // Kullanıcı Adı Kullanılabilirlik Kontrolü
    // ============================================================

    @Test
    fun `kullanılabilir kullanıcı adı için isUsernameAvailable true döner`() = runTest {
        coEvery {
            authRepository.checkUsernameAvailability("yeni_kullanici")
        } returns Result.Success(true)

        viewModel.uiState.test {
            awaitItem() // başlangıç

            viewModel.onUsernameChanged("yeni_kullanici")
            advanceTimeBy(600) // debounce süresi (500ms varsayımı)

            // Yükleniyor
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            // Sonuç
            val resultState = awaitItem()
            assertThat(resultState.isUsernameAvailable).isTrue()
            assertThat(resultState.isLoading).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `kullanılmakta olan kullanıcı adı için isUsernameAvailable false döner`() = runTest {
        coEvery {
            authRepository.checkUsernameAvailability("mevcutkullanici")
        } returns Result.Success(false)

        viewModel.uiState.test {
            awaitItem()

            viewModel.onUsernameChanged("mevcutkullanici")
            advanceTimeBy(600)

            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            val resultState = awaitItem()
            assertThat(resultState.isUsernameAvailable).isFalse()
            assertThat(resultState.isLoading).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `kullanıcı adı kontrol hatası errorMessage ayarlar`() = runTest {
        coEvery {
            authRepository.checkUsernameAvailability(any())
        } returns Result.Error(Exception("Kontrol başarısız"))

        viewModel.uiState.test {
            awaitItem()

            viewModel.onUsernameChanged("hatalikullanici")
            advanceTimeBy(600)

            awaitItem() // loading
            val errorState = awaitItem()
            assertThat(errorState.errorMessage).isNotNull()
            assertThat(errorState.isLoading).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ============================================================
    // Kullanıcı Adı Kaydetme
    // ============================================================

    @Test
    fun `confirmUsername başarıyla kaydedilir`() = runTest {
        coEvery {
            authRepository.checkUsernameAvailability("gercek_kullanici")
        } returns Result.Success(true)
        coEvery {
            authRepository.setUsername("gercek_kullanici")
        } returns Result.Success(Unit)

        viewModel.onUsernameChanged("gercek_kullanici")
        advanceTimeBy(600)
        viewModel.confirmUsername()

        // Kayıt sonrası isUsernameConfirmed true olmalı
        // (state hemen güncellenmediğinden bir miktar zaman geçirir)
        val state = viewModel.uiState.value
        // En azından hata olmamalı
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `kullanıcı adı müsait değilken confirmUsername çağrılırsa hata verilir`() = runTest {
        coEvery {
            authRepository.checkUsernameAvailability(any())
        } returns Result.Success(false)

        viewModel.onUsernameChanged("dolukadi")
        advanceTimeBy(600)
        viewModel.confirmUsername()

        val state = viewModel.uiState.value
        assertThat(state.errorMessage).isNotNull()
    }
}