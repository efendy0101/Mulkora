package com.mulkora.feature.ads

import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import android.app.Activity
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * AdMob reklam yöneticisi.
 *
 * docs/20-ads-billing-design.md: AdManager singleton sınıfı.
 * CLAUDE.md §18: "Reklamları Kaldır" satın alınmışsa hiçbir reklam gösterilmez.
 * FrequencyCapManager ile günlük gösterim limiti uygulanır.
 *
 * Kullanım:
 * ```kotlin
 * AdManager.init(context)
 * AdManager.loadInterstitial(context)
 * AdManager.showInterstitialIfReady(activity)
 * ```
 */
object AdManager {

    // --- Interstitial ---
    private var interstitialAd: InterstitialAd? = null
    private val _interstitialReady = MutableStateFlow(false)
    val interstitialReady: StateFlow<Boolean> = _interstitialReady

    // Test ID — release öncesinde ca-app-pub-XXXX/XXXX ile değiştirilecek
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // --- Rewarded ---
    private var rewardedAd: RewardedAd? = null
    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady

    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    /**
     * MobileAds'i başlatır. Application.onCreate'de çağrılmalıdır.
     */
    fun init(context: Context) {
        MobileAds.initialize(context) {}
    }

    // -----------------------------------------------------------------------
    // Interstitial
    // -----------------------------------------------------------------------

    /**
     * Interstitial reklamı önceden yükler.
     * Reklam gösterilmeden önce çağrılmalıdır.
     */
    fun loadInterstitial(context: Context) {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    _interstitialReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    _interstitialReady.value = false
                }
            },
        )
    }

    /**
     * Hazır olan interstitial reklamı gösterir.
     * [adsRemoved] true ise hiçbir şey yapılmaz.
     * Frekans limiti aşılmışsa yükleme atlanır.
     *
     * @param activity          Reklamın gösterileceği Activity
     * @param adsRemoved        Kullanıcı reklamları satın aldıysa true
     * @param onAdDismissed     Reklam kapandıktan sonra çağrılır
     */
    fun showInterstitialIfReady(
        activity: Activity,
        adsRemoved: Boolean,
        onAdDismissed: () -> Unit = {},
    ) {
        if (adsRemoved) {
            onAdDismissed()
            return
        }

        val canShow = FrequencyCapManager.canShowAd(activity)
        if (!canShow) {
            onAdDismissed()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            onAdDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                _interstitialReady.value = false
                FrequencyCapManager.recordAdShown(activity)
                onAdDismissed()
                // Bir sonraki gösterim için hazırla
                loadInterstitial(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                _interstitialReady.value = false
                onAdDismissed()
            }
        }

        ad.show(activity)
    }

    // -----------------------------------------------------------------------
    // Rewarded (bonus içerik için kullanılabilir)
    // -----------------------------------------------------------------------

    fun loadRewarded(context: Context) {
        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _rewardedReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _rewardedReady.value = false
                }
            },
        )
    }

    fun showRewardedIfReady(
        activity: Activity,
        adsRemoved: Boolean,
        onRewarded: () -> Unit = {},
        onAdDismissed: () -> Unit = {},
    ) {
        if (adsRemoved) {
            onAdDismissed()
            return
        }

        val ad = rewardedAd
        if (ad == null) {
            onAdDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _rewardedReady.value = false
                onAdDismissed()
                loadRewarded(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                _rewardedReady.value = false
                onAdDismissed()
            }
        }

        ad.show(activity) { _ ->
            onRewarded()
        }
    }
}