package com.mulkora.feature.ads

import com.mulkora.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * FrequencyCapManager birim testleri.
 *
 * CLAUDE.md §16: Reklam sıklık sınırlaması — aynı kullanıcıya belirli sürede çok sık reklam gösterilmez.
 * docs/21-testing-strategy.md — UT-ADS-001 ile UT-ADS-006.
 *
 * FrequencyCapManager'ın in-memory zamanlama mantığını test eder.
 * SharedPreferences/DataStore bağımlılıkları olmadan çalışan durum değerlendirme testleri.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FrequencyCapManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var frequencyCapManager: FrequencyCapManager

    @Before
    fun setUp() {
        frequencyCapManager = FrequencyCapManager()
    }

    // ─── UT-ADS-001: İlk gösterimde reklam gösterilebilir ────────────────────

    @Test
    fun `canShowAd returns true on first call`() {
        assertTrue(frequencyCapManager.canShowAd())
    }

    // ─── UT-ADS-002: Reklam gösterildikten hemen sonra gösterilemez ───────────

    @Test
    fun `canShowAd returns false immediately after recording an ad show`() {
        frequencyCapManager.recordAdShown()
        assertFalse(frequencyCapManager.canShowAd())
    }

    // ─── UT-ADS-003: Tekrar kayıt edilince gösterilemez ──────────────────────

    @Test
    fun `canShowAd remains false after multiple recordAdShown calls`() {
        frequencyCapManager.recordAdShown()
        frequencyCapManager.recordAdShown()
        assertFalse(frequencyCapManager.canShowAd())
    }

    // ─── UT-ADS-004: Sıfırlama sonrası tekrar gösterilebilir ─────────────────

    @Test
    fun `canShowAd returns true after reset`() = runTest {
        frequencyCapManager.recordAdShown()
        frequencyCapManager.reset()
        assertTrue(frequencyCapManager.canShowAd())
    }

    // ─── UT-ADS-005: Sıfırlamadan önce gösterilemez ───────────────────────────

    @Test
    fun `canShowAd is false before reset after recording`() = runTest {
        frequencyCapManager.recordAdShown()
        assertFalse(frequencyCapManager.canShowAd())
    }

    // ─── UT-ADS-006: Birden fazla sıfırlama çalışır ──────────────────────────

    @Test
    fun `multiple resets keep canShowAd true`() = runTest {
        frequencyCapManager.recordAdShown()
        frequencyCapManager.reset()
        frequencyCapManager.reset()
        assertTrue(frequencyCapManager.canShowAd())
    }
}