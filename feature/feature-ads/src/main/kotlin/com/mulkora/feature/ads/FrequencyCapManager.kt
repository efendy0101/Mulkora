package com.mulkora.feature.ads

import android.content.Context
import android.content.SharedPreferences

/**
 * Reklam gösterim frekansını yönetir.
 *
 * docs/20-ads-billing-design.md: FrequencyCapManager
 * CLAUDE.md §18: Günlük gösterim limiti ve cooldown süresi.
 *
 * Politika:
 * - Günlük maksimum gösterim: [MAX_DAILY_SHOWS] (varsayılan 5)
 * - İki gösterim arasında minimum bekleme: [COOLDOWN_MS] (varsayılan 60 saniye)
 * - Veriler SharedPreferences'ta saklanır; her gün sıfırlanır.
 */
object FrequencyCapManager {

    private const val PREFS_NAME = "mulkora_ad_frequency"
    private const val KEY_DAILY_COUNT = "daily_count"
    private const val KEY_LAST_SHOWN_MS = "last_shown_ms"
    private const val KEY_DATE_EPOCH_DAY = "date_epoch_day"

    /** Günde en fazla kaç kez interstitial gösterilebilir. */
    private const val MAX_DAILY_SHOWS = 5

    /** İki gösterim arasındaki minimum süre (ms). CLAUDE.md §17: 10 dakika. */
    private const val COOLDOWN_MS = 10 * 60_000L // 10 dakika

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Bugünkü epoch gün numarasını hesaplar.
     * Gün değiştiğinde sayaç sıfırlanır.
     */
    private fun todayEpochDay(): Long =
        System.currentTimeMillis() / (24 * 60 * 60 * 1000L)

    /**
     * Reklam gösterilebilir mi?
     *
     * Koşullar:
     * 1. Bugün [MAX_DAILY_SHOWS] limitine ulaşılmamış olmalı.
     * 2. Son gösterimden bu yana [COOLDOWN_MS] geçmiş olmalı.
     */
    fun canShowAd(context: Context): Boolean {
        val prefs = prefs(context)
        val today = todayEpochDay()

        // Gün sıfırlama
        val savedDay = prefs.getLong(KEY_DATE_EPOCH_DAY, -1L)
        if (savedDay != today) {
            prefs.edit()
                .putLong(KEY_DATE_EPOCH_DAY, today)
                .putInt(KEY_DAILY_COUNT, 0)
                .apply()
        }

        val dailyCount = prefs.getInt(KEY_DAILY_COUNT, 0)
        if (dailyCount >= MAX_DAILY_SHOWS) return false

        val lastShown = prefs.getLong(KEY_LAST_SHOWN_MS, 0L)
        val elapsed = System.currentTimeMillis() - lastShown
        return elapsed >= COOLDOWN_MS
    }

    /**
     * Reklam gösterildi; sayaç ve zaman damgasını güncelle.
     */
    fun recordAdShown(context: Context) {
        val prefs = prefs(context)
        val today = todayEpochDay()
        val savedDay = prefs.getLong(KEY_DATE_EPOCH_DAY, -1L)
        val currentCount = if (savedDay == today) prefs.getInt(KEY_DAILY_COUNT, 0) else 0

        prefs.edit()
            .putLong(KEY_DATE_EPOCH_DAY, today)
            .putInt(KEY_DAILY_COUNT, currentCount + 1)
            .putLong(KEY_LAST_SHOWN_MS, System.currentTimeMillis())
            .apply()
    }

    /**
     * Sıfırlama (test amaçlı).
     */
    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
    }
}