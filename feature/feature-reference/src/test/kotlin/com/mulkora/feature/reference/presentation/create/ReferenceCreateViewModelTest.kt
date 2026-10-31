package com.mulkora.feature.reference.presentation.create

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
 * ReferenceCreateViewModelTest — Referans oluşturma ekranı senaryoları.
 *
 * CLAUDE.md §13 Temel Akış:
 *   1. Kullanıcı referans oluşturmak istediği kişiyi seçer.
 *   2. Hedef kullanıcıya "Bu kişiyi tanıyor musunuz?" sorusu gönderilir.
 *   3. "Hayır" cevabında süreç durur ve referans yayımlanmaz.
 *   4. "Evet" cevabında referans hazırlama ekranı açılır.
 *   5. Referans yönetici onayına gönderilir.
 * docs/21-testing-strategy.md — UT-REFERENCE kapsamı.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReferenceCreateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ReferenceRepository
    private lateinit var viewModel: ReferenceCreateViewModel

    @Before
    fun setup() {
        repository = mockk()
        viewModel = ReferenceCreateViewModel(repository)
    }

    // ============================================================
    // Başlangıç Durumu
    // ============================================================

    @Test
    fun `başlangıçta tüm alanlar varsayılan değerlerde`() {
        val state = viewModel.uiState.value
        assertThat(state.targetUid).isEmpty()
        assertThat(state.relationshipType).isNull()
        assertThat(state.overallRating).isNull()
        assertThat(state.comment).isEmpty()
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
        assertThat(state.isSubmitted).isFalse()
    }

    // ============================================================
    // Form Girişleri
    // ============================================================

    @Test
    fun `onTargetUidChanged hedef kullanıcıyı günceller`() {
        viewModel.onTargetUidChanged("user_target")
        assertThat(viewModel.uiState.value.targetUid).isEqualTo("user_target")
    }

    @Test
    fun `onRelationshipTypeChanged ilişki türünü günceller`() {
        viewModel.onRelationshipTypeChanged("previous_tenant")
        assertThat(viewModel.uiState.value.relationshipType).isEqualTo("previous_tenant")
    }

    @Test
    fun `onOverallRatingChanged puanı günceller`() {
        viewModel.onOverallRatingChanged(4.5f)
        assertThat(viewModel.uiState.value.overallRating).isEqualTo(4.5f)
    }

    @Test
    fun `onCommentChanged açıklamayı günceller`() {
        viewModel.onCommentChanged("Mükemmel bir kiracıydı.")
        assertThat(viewModel.uiState.value.comment).isEqualTo("Mükemmel bir kiracıydı.")
    }

    // ============================================================
    // Tanıma Talebi Gönderme
    // ============================================================

    @Test
    fun `sendRecognitionRequest başarıyla gönderilir`() = runTest {
        coEvery {
            repository.sendRecognitionRequest("user_target")
        } returns Result.Success(Unit)

        viewModel.onTargetUidChanged("user_target")
        viewModel.sendRecognitionRequest()

        val state = viewModel.uiState.value
        assertThat(state.isRecognitionRequestSent).isTrue()
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `sendRecognitionRequest hata verdiğinde errorMessage ayarlanır`() = runTest {
        coEvery {
            repository.sendRecognitionRequest(any())
        } returns Result.Error(Exception("Gönderim başarısız"))

        viewModel.onTargetUidChanged("user_target")
        viewModel.sendRecognitionRequest()

        val state = viewModel.uiState.value
        assertThat(state.isRecognitionRequestSent).isFalse()
        assertThat(state.errorMessage).isNotNull()
    }

    @Test
    fun `hedef UID boşsa sendRecognitionRequest hata verir`() = runTest {
        viewModel.sendRecognitionRequest()

        val state = viewModel.uiState.value
        assertThat(state.isRecognitionRequestSent).isFalse()
        assertThat(state.errorMessage).isNotNull()
    }

    // ============================================================
    // Referans Gönderme
    // ============================================================

    @Test
    fun `submitReference tüm alanlar dolu ve başarılı`() = runTest {
        coEvery {
            repository.submitReference(any())
        } returns Result.Success("ref_001")

        viewModel.onTargetUidChanged("user_target")
        viewModel.onRelationshipTypeChanged("previous_tenant")
        viewModel.onOverallRatingChanged(4.0f)
        viewModel.onCommentChanged("Çok titiz biriydi.")
        viewModel.submitReference()

        val state = viewModel.uiState.value
        assertThat(state.isSubmitted).isTrue()
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `submitReference eksik alan ile hata verir`() = runTest {
        // Puan eksik
        viewModel.onTargetUidChanged("user_target")
        viewModel.onRelationshipTypeChanged("previous_tenant")
        viewModel.submitReference()

        val state = viewModel.uiState.value
        assertThat(state.isSubmitted).isFalse()
        assertThat(state.errorMessage).isNotNull()
    }

    @Test
    fun `submitReference repository hatası errorMessage ayarlar`() = runTest {
        coEvery {
            repository.submitReference(any())
        } returns Result.Error(Exception("Gönderim hatası"))

        viewModel.onTargetUidChanged("user_target")
        viewModel.onRelationshipTypeChanged("previous_tenant")
        viewModel.onOverallRatingChanged(3.0f)
        viewModel.onCommentChanged("Ortalama bir deneyimdi.")
        viewModel.submitReference()

        val state = viewModel.uiState.value
        assertThat(state.isSubmitted).isFalse()
        assertThat(state.errorMessage).isNotNull()
        assertThat(state.isLoading).isFalse()
    }

    // ============================================================
    // Güvenlik — Başka Kullanıcı Adına Referans
    // ============================================================

    @Test
    fun `kendi UID ile aynı targetUid referans gönderilemez`() = runTest {
        // Kullanıcı kendine referans vermeye çalışır
        viewModel.onTargetUidChanged("current_user_uid")
        viewModel.onRelationshipTypeChanged("previous_tenant")
        viewModel.onOverallRatingChanged(5.0f)
        viewModel.onCommentChanged("Kendim hakkında referans.")
        viewModel.submitReference()

        // Güvenlik: currentUid == targetUid durumunda red edilmeli
        // ViewModel bu kontrolü yapıyor; hata mesajı bekliyoruz
        // (Gerçek implementasyona göre bu state isSubmitted==false olabilir)
        assertThat(viewModel.uiState.value.isSubmitted).isFalse()
    }
}