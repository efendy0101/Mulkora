package com.mulkora.feature.billing.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.mulkora.core.common.result.Result
import com.mulkora.feature.billing.domain.model.BillingProductIds
import com.mulkora.feature.billing.domain.model.PurchaseStatus
import com.mulkora.feature.billing.domain.repository.BillingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * BillingRepository'nin Google Play Billing + Cloud Functions tabanlı gerçek uygulaması.
 *
 * CLAUDE.md §18:
 * - Satın alma hakları Firestore `purchases/{uid}` belgesinden okunur.
 * - Token backend Cloud Function `validatePurchaseToken` ile doğrulanır.
 * - İstemci tarafında satın alma durumu değiştirilemez (Firestore Rules: allow write: if false).
 * - Uygulama yeniden kurulsa bile satın alma geri yüklenir (Play hesabı sorgusu).
 *
 * Mimari not: launchRemoveAdsPurchase Activity referansı gerektirir.
 * Activity, ActivityHolder (below) aracılığıyla enjekte edilir.
 */
@Singleton
class BillingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
) : BillingRepository, PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null
    private var removeAdsProductDetails: ProductDetails? = null

    /**
     * Satın alma tamamlandığında çağrılır; token'ı backend'e gönderir.
     * Activity, setActivity() ile set edilir.
     */
    private var currentActivity: Activity? = null

    /** Activity referansını set eder. RemoveAdsScreen'de çağrılmalıdır. */
    override fun setActivity(activity: Activity?) {
        currentActivity = activity
    }

    // ---- BillingRepository -----------------------------------------------

    override fun observePurchaseStatus(): Flow<PurchaseStatus> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(PurchaseStatus.NOT_PURCHASED)
            close()
            return@callbackFlow
        }

        val docRef = firestore
            .collection("purchases")
            .document(uid)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(PurchaseStatus.UNKNOWN)
                return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) {
                trySend(PurchaseStatus.NOT_PURCHASED)
                return@addSnapshotListener
            }
            val statusStr = snapshot.getString("status") ?: "NOT_PURCHASED"
            val status = try {
                PurchaseStatus.valueOf(statusStr.uppercase())
            } catch (_: IllegalArgumentException) {
                PurchaseStatus.UNKNOWN
            }
            trySend(status)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun initializeBilling(): Result<Unit> {
        return suspendCancellableCoroutine { cont ->
            val client = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases()
                .build()
            billingClient = client

            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingResponseCode.OK) {
                        // Ürün detaylarını yükle
                        val params = QueryProductDetailsParams.newBuilder()
                            .setProductList(
                                listOf(
                                    QueryProductDetailsParams.Product.newBuilder()
                                        .setProductId(BillingProductIds.REMOVE_ADS_LIFETIME)
                                        .setProductType(BillingClient.ProductType.INAPP)
                                        .build(),
                                ),
                            )
                            .build()

                        client.queryProductDetailsAsync(params) { _, productDetailsList ->
                            removeAdsProductDetails = productDetailsList
                                .firstOrNull { it.productId == BillingProductIds.REMOVE_ADS_LIFETIME }
                        }

                        if (cont.isActive) cont.resume(Result.Success(Unit))
                    } else {
                        if (cont.isActive) {
                            cont.resume(
                                Result.Error(
                                    Exception("Billing bağlantısı kurulamadı: ${result.debugMessage}")
                                )
                            )
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // WorkManager veya retry mekanizması eklenebilir
                }
            })
        }
    }

    override suspend fun launchRemoveAdsPurchase(): Result<Unit> {
        val activity = currentActivity
            ?: return Result.Error(Exception("Activity referansı yok; setActivity() çağrılmamış"))

        val client = billingClient
            ?: return Result.Error(Exception("Billing başlatılmamış; initializeBilling() çağrılmalı"))

        if (!client.isReady) {
            return Result.Error(Exception("BillingClient henüz hazır değil"))
        }

        val productDetails = removeAdsProductDetails
            ?: return Result.Error(Exception("Ürün ayrıntıları yüklenemedi; ürün Play Console'da tanımlı mı?"))

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        // launchBillingFlow ana thread'de çalışmalıdır; Compose Activity içinden çağrılır.
        val billingResult = client.launchBillingFlow(activity, billingFlowParams)

        return if (billingResult.responseCode == BillingResponseCode.OK) {
            Result.Success(Unit)
        } else {
            Result.Error(Exception("Satın alma akışı başlatılamadı: ${billingResult.debugMessage}"))
        }
    }

    /**
     * Google Play tarafından dönen satın almayı işler.
     * Önce acknowledge, sonra Cloud Function ile token doğrular.
     *
     * CLAUDE.md §18: Google Play satın alma tokenı backend üzerinden doğrulanmalıdır.
     * Firestore Rules: purchases/{uid} → allow write: if false  (istemci yazamaz)
     */
    override suspend fun processPurchaseToken(purchaseToken: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.Error(Exception("Kullanıcı oturumu yok"))

            // 1. Satın almayı acknowledge et (zorunlu — 3 gün içinde yapılmadan iade edilir)
            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
            billingClient?.acknowledgePurchase(acknowledgeParams) { _ -> }

            // 2. Token'ı Cloud Function ile doğrula ve Firestore'a yaz (CF Admin SDK ile)
            // CLAUDE.md §18: İstemci tarafında satın alma durumu değiştirilemez.
            functions
                .getHttpsCallable("validatePurchaseToken")
                .call(
                    mapOf(
                        "purchaseToken" to purchaseToken,
                        "productId" to BillingProductIds.REMOVE_ADS_LIFETIME,
                        "uid" to uid,
                    )
                )
                .await()

            Result.Success(Unit)
        } catch (e: FirebaseFunctionsException) {
            Result.Error(e, "Satın alma doğrulanamadı: ${e.message}")
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun restorePurchases(): Result<Unit> {
        val client = billingClient
            ?: return Result.Error(Exception("Billing başlatılmamış"))

        if (!client.isReady) {
            return Result.Error(Exception("BillingClient henüz hazır değil"))
        }

        return suspendCancellableCoroutine { cont ->
            val queryParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            client.queryPurchasesAsync(queryParams) { billingResult, purchases ->
                if (billingResult.responseCode != BillingResponseCode.OK) {
                    if (cont.isActive) {
                        cont.resume(
                            Result.Error(Exception("Satın almalar sorgulanamadı: ${billingResult.debugMessage}"))
                        )
                    }
                    return@queryPurchasesAsync
                }

                // Her aktif satın almayı backend ile doğrula
                val activePurchases = purchases.filter {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        it.products.contains(BillingProductIds.REMOVE_ADS_LIFETIME)
                }

                if (activePurchases.isEmpty()) {
                    if (cont.isActive) cont.resume(Result.Success(Unit))
                    return@queryPurchasesAsync
                }

                // Her satın alma için CF doğrulama tetikle (async — result beklenmez)
                for (purchase in activePurchases) {
                    functions.getHttpsCallable("validatePurchaseToken")
                        .call(
                            mapOf(
                                "purchaseToken" to purchase.purchaseToken,
                                "productId" to BillingProductIds.REMOVE_ADS_LIFETIME,
                            )
                        )
                }

                if (cont.isActive) cont.resume(Result.Success(Unit))
            }
        }
    }

    override fun disconnectBilling() {
        billingClient?.endConnection()
        billingClient = null
        currentActivity = null
    }

    // ---- PurchasesUpdatedListener -----------------------------------------

    /**
     * Google Play satın alma güncellemesi geldiğinde çağrılır.
     * Her başarılı satın almayı backend'e gönderir.
     */
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        // Arka planda token doğrulaması başlat
                        functions.getHttpsCallable("validatePurchaseToken")
                            .call(
                                mapOf(
                                    "purchaseToken" to purchase.purchaseToken,
                                    "productId" to BillingProductIds.REMOVE_ADS_LIFETIME,
                                )
                            )
                    }
                }
            }
            BillingResponseCode.USER_CANCELED -> {
                // Kullanıcı iptal etti — herhangi bir işlem gerekmez
            }
            else -> {
                // Diğer hatalar — loglama eklenebilir
            }
        }
    }
}
