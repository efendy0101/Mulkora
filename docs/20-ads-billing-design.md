# Mülkora — Reklam ve Satın Alma Tasarımı

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## 1. Reklam Türleri

| Tür | Konum | Frekans |
|---|---|---|
| Banner Reklam | Ana akış altı, gönderi detayı altı | Sürekli görünür |
| Native Reklam | Ana akış içinde liste arasında | Her 9. içerik sonrası |
| Interstitial Reklam | Ekran geçişlerinde (gönderi oluşturma sonrası vb.) | 10 dakikada 1, günde max 5 |

Kullanıcı "Reklamları Kaldır" satın alırsa tüm reklam türleri gizlenir.

---

## 2. AdMob Entegrasyonu

### Build Varyantına Göre Reklam ID Yönetimi

```kotlin
// BuildConfig alanları
// debug: test reklam ID'leri
// release: gerçek reklam ID'leri

object AdUnitIds {
    val BANNER = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111"  // Test
    } else {
        BuildConfig.ADMOB_BANNER_ID  // Production (local.properties'den)
    }
    
    val NATIVE = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2247696110"  // Test
    } else {
        BuildConfig.ADMOB_NATIVE_ID
    }
    
    val INTERSTITIAL = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/1033173712"  // Test
    } else {
        BuildConfig.ADMOB_INTERSTITIAL_ID
    }
}
```

**Önemli:** Gerçek reklam ID'leri asla kaynak koduna yazılmaz; `local.properties` üzerinden `BuildConfig` alanlarına aktarılır ve `.gitignore` kapsamında tutulur.

---

## 3. Frekans Yönetimi (FrequencyCapManager)

```kotlin
// FrequencyCapManager.kt
class FrequencyCapManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val LAST_INTERSTITIAL_KEY = longPreferencesKey("last_interstitial_time")
        val DAILY_INTERSTITIAL_COUNT_KEY = intPreferencesKey("daily_interstitial_count")
        val INTERSTITIAL_DATE_KEY = stringPreferencesKey("interstitial_date")
        
        const val INTERSTITIAL_COOLDOWN_MS = 10 * 60 * 1000L  // 10 dakika
        const val DAILY_INTERSTITIAL_LIMIT = 5
    }
    
    suspend fun canShowInterstitial(): Boolean {
        val prefs = dataStore.data.first()
        val lastTime = prefs[LAST_INTERSTITIAL_KEY] ?: 0L
        val today = LocalDate.now().toString()
        val storedDate = prefs[INTERSTITIAL_DATE_KEY] ?: ""
        
        // Tarih değiştiyse sayacı sıfırla
        val dailyCount = if (storedDate == today) {
            prefs[DAILY_INTERSTITIAL_COUNT_KEY] ?: 0
        } else {
            0
        }
        
        val cooldownPassed = System.currentTimeMillis() - lastTime > INTERSTITIAL_COOLDOWN_MS
        val dailyLimitOk = dailyCount < DAILY_INTERSTITIAL_LIMIT
        
        return cooldownPassed && dailyLimitOk
    }
    
    suspend fun recordInterstitialShown() {
        val today = LocalDate.now().toString()
        dataStore.edit { prefs ->
            prefs[LAST_INTERSTITIAL_KEY] = System.currentTimeMillis()
            
            val storedDate = prefs[INTERSTITIAL_DATE_KEY] ?: ""
            val currentCount = if (storedDate == today) {
                prefs[DAILY_INTERSTITIAL_COUNT_KEY] ?: 0
            } else {
                0
            }
            
            prefs[INTERSTITIAL_DATE_KEY] = today
            prefs[DAILY_INTERSTITIAL_COUNT_KEY] = currentCount + 1
        }
    }
}
```

---

## 4. Native Reklam Akış Entegrasyonu

Ana akıştaki içerik listesine her 9. gönderiden sonra native reklam eklenir. İlk 4 gönderi reklamdan muaftır (soğuk başlangıç UX).

```kotlin
// FeedListWithAds.kt
@Composable
fun FeedListWithAds(
    posts: LazyPagingItems<FeedItem>,
    adManager: AdManager,
    adsRemoved: Boolean
) {
    LazyColumn {
        items(posts.itemCount) { index ->
            when (val item = posts[index]) {
                is FeedItem.Post -> PostCard(post = item.post)
                is FeedItem.NativeAd -> {
                    if (!adsRemoved) {
                        NativeAdCard(ad = item.ad)
                    }
                }
            }
        }
    }
}

// FeedItem.kt — Gönderi ve Reklam union tipi
sealed class FeedItem {
    data class Post(val post: PostDto) : FeedItem()
    data class NativeAd(val ad: NativeAdData) : FeedItem()
}
```

Native reklam ekleme mantığı `PostPagingSource` veya `FeedViewModel` içinde sayfalama verisi dönerken uygulanır.

---

## 5. Google Play Billing — Satın Alma Akışı

### Ürün Tanımı

```
Ürün ID: remove_ads_lifetime
Tür: Tek seferlik satın alma (ONE_TIME)
Fiyat: [Google Play Console'da belirlenir]
```

### Android BillingClient Kurulumu

```kotlin
// BillingClientWrapper.kt
class BillingClientWrapper @Inject constructor(
    private val context: Context
) {
    private val billingClient = BillingClient.newBuilder(context)
        .setListener { billingResult, purchases ->
            handlePurchaseUpdates(billingResult, purchases)
        }
        .enablePendingPurchases()
        .build()

    suspend fun startConnection(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }
                override fun onBillingServiceDisconnected() {
                    continuation.resume(false)
                }
            })
        }
    }

    suspend fun queryProductDetails(): ProductDetails? {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId("remove_ads_lifetime")
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            ))
            .build()
        
        val result = billingClient.queryProductDetails(params)
        return result.productDetailsList?.firstOrNull()
    }
    
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        billingClient.launchBillingFlow(activity, billingFlowParams)
    }
    
    private fun handlePurchaseUpdates(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    // Backend doğrulamasına gönder
                    verifyAndAcknowledgePurchase(purchase)
                }
            }
        }
    }
}
```

---

## 6. Satın Alma Doğrulama Akışı

```
Android Uygulama                    Cloud Functions               Google Play API
        │                                   │                           │
        │ launchBillingFlow()               │                           │
        │──────────────────────────────────>│                           │
        │                                   │                           │
        │ Purchase.purchaseToken            │                           │
        │─────────────────────────────────>Cloud Function               │
        │                                   │── GET products/tokens/ ──>│
        │                                   │<── purchaseState:0 ───────│
        │                                   │                           │
        │                                   │ (Token replay attack?)    │
        │                                   │ (token daha önce var mı?) │
        │                                   │                           │
        │                                   │── purchases/{uid} oluştur │
        │                                   │── users/{uid}.adsRemoved  │
        │                                   │   = true                  │
        │                                   │                           │
        │<── { success: true } ─────────────│                           │
        │                                   │                           │
        │ billingClient.acknowledgePurchase()                           │
        │ (onay gönder, 3 günlük süre)      │                           │
        │──────────────────────────────────>│                           │
        │                                   │                           │
        │ UI: Reklamları gizle              │                           │
```

**Kritik:** Satın alma doğrulanmadan `billingClient.acknowledgePurchase()` çağrılmaz. Onay yapılmadan 3 gün geçerse Google Play otomatik iade eder.

---

## 7. Satın Alma Geri Yükleme

```kotlin
// BillingRepository.kt
suspend fun restorePurchases(): RestoreResult {
    val params = QueryPurchasesParams.newBuilder()
        .setProductType(BillingClient.ProductType.INAPP)
        .build()
    
    val result = billingClient.queryPurchasesAsync(params)
    
    for (purchase in result.purchasesList) {
        if (purchase.products.contains("remove_ads_lifetime")
            && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Backend'e doğrulatarak aktif olarak işaretle
            val verified = cloudFunctions.verifyPurchase(
                purchaseToken = purchase.purchaseToken,
                productId = "remove_ads_lifetime"
            )
            if (verified) return RestoreResult.Success
        }
    }
    return RestoreResult.NoPurchasesFound
}
```

---

## 8. Reklam Görünürlük Kararı

```kotlin
// AdManager.kt
@Singleton
class AdManager @Inject constructor(
    private val userRepository: UserRepository,
    private val frequencyCapManager: FrequencyCapManager,
    private val remoteConfig: FirebaseRemoteConfig
) {
    // Firestore'dan adsRemoved durumunu dinle
    val adsRemovedFlow: StateFlow<Boolean> = userRepository
        .currentUserFlow
        .map { it?.adsRemoved == true }
        .stateIn(GlobalScope, SharingStarted.Eagerly, false)
    
    suspend fun shouldShowInterstitial(): Boolean {
        if (adsRemovedFlow.value) return false
        return frequencyCapManager.canShowInterstitial()
    }
    
    fun getNativeAdFrequency(): Int {
        return remoteConfig.getLong("native_ad_frequency").toInt()
            .takeIf { it > 0 } ?: 9
    }
    
    fun getInitialPostsWithoutAd(): Int {
        return remoteConfig.getLong("initial_posts_without_ad").toInt()
            .takeIf { it >= 0 } ?: 4
    }
}
```

---

## 9. Firebase Remote Config Entegrasyonu

Reklam frekansı ve satın alma fiyatı bilgisi (sadece gösterim için) Remote Config ile yönetilir; uygulamayı güncellemeden değiştirilebilir.

```kotlin
// RemoteConfigKeys.kt
object RemoteConfigKeys {
    const val NATIVE_AD_FREQUENCY = "native_ad_frequency"
    const val INTERSTITIAL_COOLDOWN_MINUTES = "interstitial_cooldown_minutes"
    const val DAILY_INTERSTITIAL_LIMIT = "daily_interstitial_limit"
    const val INITIAL_POSTS_WITHOUT_AD = "initial_posts_without_ad"
    const val REMOVE_ADS_PRICE_DISPLAY = "remove_ads_price_display"
    const val REMOVE_ADS_FEATURE_ENABLED = "remove_ads_feature_enabled"
}
```

---

## 10. Önemli Güvenlik Notları

- Satın alma tokenı **hiçbir zaman** istemci tarafında doğrulanmaz; doğrulama yalnızca Cloud Functions'da yapılır.
- `adsRemoved` alanı Firestore Security Rules kapsamındadır; kullanıcı doğrudan yazamaz.
- Google Play RTDN webhook ile iade durumları anlık izlenir.
- Test cihazlarında AdMob test reklam ID'leri kullanılır; üretim ID'leri yalnızca imzalı production build'lerde aktiftir.

---

*Sonraki doküman: 21-testing-strategy.md*