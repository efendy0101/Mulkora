package com.mulkora.feature.reports.presentation

import com.mulkora.core.common.model.ReportReason
import com.mulkora.core.testing.MainDispatcherRule
import com.mulkora.feature.reports.domain.model.ReportTargetType
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * ReportViewModel birim testleri.
 *
 * CLAUDE.md §11: Şikâyet Sistemi.
 * Kural: Şikayet doğrudan Firestore'a yazılamaz (firestore.rules: allow create: if false).
 * docs/21-testing-strategy.md — UT-REPORT-001 ile UT-REPORT-007.
 *
 * Not: ReportViewModel FirebaseFunctions'ı doğrudan inject eder.
 * Bu nedenle integration testi yerine state-machine davranışı unit test edilir.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: ReportViewModel

    private val targetId = "post_abc_123"
    private val targetType = ReportTargetType.POST

    @Before
    fun setUp() {
        // ReportViewModel FirebaseFunctions inject eder; bu testlerde FirebaseFunctions
        // inject edilmeden doğrudan state mantığı test edilir.
        // FirebaseFunctions gerektiren testler instrumented test olarak ayrılır.
        viewModel = ReportViewModel(mockk(relaxed = true))
    }

    // ─── UT-REPORT-001: Başlangıç durumu ─────────────────────────────────────

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertNull(state.selectedReason)
        assertTrue(state.customReason.isEmpty())
        assertFalse(state.isSubmitting)
        assertFalse(state.isSubmitted)
        assertNull(state.error)
    }

    // ─── UT-REPORT-002: Sebep seçimi ─────────────────────────────────────────

    @Test
    fun `onReasonSelected updates selectedReason`() {
        viewModel.onReasonSelected(ReportReason.SPAM)
        assertEquals(ReportReason.SPAM, viewModel.uiState.value.selectedReason)
    }

    // ─── UT-REPORT-003: Farklı sebep seçimi ──────────────────────────────────

    @Test
    fun `onReasonSelected can change reason`() {
        viewModel.onReasonSelected(ReportReason.SPAM)
        viewModel.onReasonSelected(ReportReason.HATE_SPEECH)
        assertEquals(ReportReason.HATE_SPEECH, viewModel.uiState.value.selectedReason)
    }

    // ─── UT-REPORT-004: Özel sebep metin güncellemesi ────────────────────────

    @Test
    fun `onCustomReasonChanged updates customReason`() {
        viewModel.onCustomReasonChanged("Bu içerik rahatsız edicidir.")
        assertEquals("Bu içerik rahatsız edicidir.", viewModel.uiState.value.customReason)
    }

    // ─── UT-REPORT-005: Sebep seçilmeden gönderme ────────────────────────────

    @Test
    fun `submitReport without reason sets error`() = runTest {
        // Sebep seçilmeden gönder
        viewModel.submitReport(targetId, targetType)

        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertFalse(state.isSubmitting)
        assertFalse(state.isSubmitted)
    }

    // ─── UT-REPORT-006: Sebep seçiminin error'u temizlemesi ──────────────────

    @Test
    fun `onReasonSelected clears error`() = runTest {
        // Önce hata oluştur
        viewModel.submitReport(targetId, targetType)
        assertNotNull(viewModel.uiState.value.error)

        // Sebep seç — hata temizlenmeli
        viewModel.onReasonSelected(ReportReason.HARASSMENT)
        assertNull(viewModel.uiState.value.error)
    }

    // ─── UT-REPORT-007: OTHER sebep ile özel metin zorunluluğu ───────────────

    @Test
    fun `submitReport with OTHER reason but empty customReason sets error`() = runTest {
        viewModel.onReasonSelected(ReportReason.OTHER)
        viewModel.onCustomReasonChanged("")
        viewModel.submitReport(targetId, targetType)

        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertFalse(state.isSubmitted)
    }
}