package com.mulkora.feature.billing.domain.repository

import android.app.Activity
import com.mulkora.core.common.result.Result
import com.mulkora.feature.billing.domain.model.PurchaseStatus
import kotlinx.coroutines.flow.Flow

/**
 * Faturalandırma deposu sözleşmesi.
 *
 * CLAUDE.md §18:
 * - Satın alma durumu yalnızca yerel depolamada tutulmamalıdır.
 * - Google Play satın alma tokenı backend üzerinden doğrulanmalıdır.
 * - Uygulama yeniden kurulsa bile satın alma geri yüklenir.
 * - "Satın Almaları Geri Yükle" seçeneği bulunur.
 * - İade edilen satın almayı işleme: Cloud Function üzerinden.
 */
interface BillingRepository {

    /**
     * Anlık satın alma durumunu gözlemler.
     * Firestore'daki `purchases/{uid}` belgesini dinler.
     * Backend doğrulamasından geçmiş durumu döner.
     */
    fun observePurchaseStatus(): Flow<PurchaseStatus>

    /**
     * Satın alma akışını başlatmak için gereken Activity referansını set eder.
     * RemoveAdsScreen'de DisposableEffect ile çağrılmalı, onDispose'da null geçilmeli.
     */
    fun setActivity(activity: Activity?)

    /**
     * Google Play Billing bağlantısını başlatır ve mevcut
     * satın almaları sorgular. Uygulama başlangıcında çağrılır.
     */
    suspend fun initializeBilling(): Result<Unit>

    /**
     * Reklam kaldırma ürününü satın alma akışını başlatır.
     * Play Billing Library launch flow — Activity gerektirir.
     * Sonuç [processPurchaseToken] aracılığıyla işlenir.
     *
     * @return Akış başlatılabiliyorsa Success, değilse Error.
     */
    suspend fun launchRemoveAdsPurchase(): Result<Unit>

    /**
     * Google Play'den dönen satın alma tokenını backend'e gönderir.
     * Cloud Function `validatePurchaseToken` çağrılır ve
     * Firestore'daki `purchases` belgesi güncellenir.
     *
     * CLAUDE.md §18: Satın alma hakları istemciden değiştirilemez.
     */
    suspend fun processPurchaseToken(purchaseToken: String): Result<Unit>

    /**
     * Aynı Google Play hesabıyla daha önce yapılmış satın almaları
     * sorgular ve backend'e doğrulama talebi gönderir.
     *
     * CLAUDE.md §18: Başka cihazda aynı Google Play hesabıyla doğrulanabilir.
     */
    suspend fun restorePurchases(): Result<Unit>

    /**
     * Bağlantıyı kapatır. ViewModel temizlendiğinde çağrılır.
     */
    fun disconnectBilling()
}