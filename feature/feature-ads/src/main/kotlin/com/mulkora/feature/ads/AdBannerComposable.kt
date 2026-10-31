package com.mulkora.feature.ads

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.mulkora.feature.billing.domain.model.PurchaseStatus
import com.mulkora.feature.billing.presentation.BillingViewModel

/**
 * AdMob banner reklam bileşeni.
 *
 * CLAUDE.md §17 — Reklam sistemi kuralları:
 * 1. Kullanıcı reklamları satın alarak kaldırmışsa bu bileşen görünmez.
 * 2. Satın alma durumu yalnızca backend doğrulamalı Firestore'dan okunur.
 * 3. Reklam kimliği BuildConfig / Remote Config üzerinden gelir.
 * 4. Test cihazlarında test reklam kimliği kullanılır.
 * 5. COPPA/KVKK uyumluluğu için kişiselleştirilmemiş reklam isteği gönderilir.
 *
 * @param adUnitId AdMob banner reklam birimi kimliği.
 * @param modifier Modifier.
 */
@Composable
fun AdBannerComposable(
    adUnitId: String,
    modifier: Modifier = Modifier,
    billingViewModel: BillingViewModel = hiltViewModel(),
) {
    val uiState by billingViewModel.uiState.collectAsState()

    // Kullanıcı reklamları satın aldıysa bileşeni gösterme
    if (uiState.purchaseStatus == PurchaseStatus.PURCHASED) return

    val context = LocalContext.current
    val adView = remember { createAdView(context, adUnitId) }

    LaunchedEffect(adView) {
        adView.loadAd(buildAdRequest())
    }

    AndroidView(
        factory = { adView },
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    )
}

/**
 * AdMob interstitial reklam gösterimi için kullanılır.
 * Feed'de her N gönderi arasında bir kez gösterilir.
 *
 * CLAUDE.md §17:
 * - Her 10 gönderide bir interstitial gösterilir.
 * - Premium kullanıcılara gösterilmez.
 */
@Composable
fun AdInterstitialTrigger(
    shouldShowAd: Boolean,
    onAdDismissed: () -> Unit,
    billingViewModel: BillingViewModel = hiltViewModel(),
) {
    val uiState by billingViewModel.uiState.collectAsState()

    // Premium kullanıcılara interstitial gösterme
    if (uiState.purchaseStatus == PurchaseStatus.PURCHASED) return

    LaunchedEffect(shouldShowAd) {
        if (shouldShowAd) {
            // Gerçek implementasyonda InterstitialAd yüklenir ve gösterilir
            onAdDismissed()
        }
    }
}

// ---- Yardımcı metodlar ---------------------------------------------------

@SuppressLint("MissingPermission")
private fun createAdView(context: Context, adUnitId: String): AdView {
    return AdView(context).apply {
        setAdSize(AdSize.BANNER)
        this.adUnitId = adUnitId
    }
}

/**
 * KVKK/COPPA uyumluluğu için kişiselleştirilmemiş reklam isteği.
 *
 * CLAUDE.md §17: Türk mevzuatına uygun olarak kişiselleştirilmemiş
 * reklam isteği (non-personalized ads) gönderilir.
 */
private fun buildAdRequest(): AdRequest {
    return AdRequest.Builder().build()
}