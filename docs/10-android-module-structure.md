# Mülkora — Android Modül Yapısı

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## 1. Modüler Mimari Genel Bakış

Mülkora, Clean Architecture + MVVM temelinde çok modüllü (multi-module) bir yapıya sahiptir. Her feature kendi modülünde izole edilir; modüller arası bağımlılıklar yalnızca `core-*` modülleri üzerinden kurulur.

```
:app
│
├── core/
│   ├── :core-common
│   ├── :core-ui
│   ├── :core-designsystem
│   ├── :core-network
│   ├── :core-database
│   ├── :core-firebase
│   └── :core-testing
│
└── feature/
    ├── :feature-auth
    ├── :feature-onboarding
    ├── :feature-feed
    ├── :feature-explore
    ├── :feature-post-create
    ├── :feature-post-detail
    ├── :feature-comments
    ├── :feature-reactions
    ├── :feature-reports
    ├── :feature-references
    ├── :feature-contracts
    ├── :feature-notifications
    ├── :feature-profile
    ├── :feature-settings
    ├── :feature-billing
    └── :feature-ads
```

---

## 2. Bağımlılık Kuralları

```
:app → tüm :feature-* modülleri + tüm :core-* modülleri
:feature-* → :core-common, :core-ui, :core-designsystem, :core-firebase
:core-ui → :core-common, :core-designsystem
:core-firebase → :core-common
:core-database → :core-common
:core-network → :core-common
:core-testing → :core-common (yalnızca test scope)

Kural: :feature-A hiçbir zaman :feature-B'ye doğrudan bağımlı olamaz.
Kural: :core-* modülleri birbirlerine dairesel bağımlılık kuramaz.
```

---

## 3. Katman Mimarisi (Her Feature Modülünde)

```
feature-xyz/
├── data/
│   ├── datasource/
│   │   ├── XyzRemoteDataSource.kt       (Firestore / Cloud Functions)
│   │   └── XyzLocalDataSource.kt        (Room / DataStore)
│   ├── dto/
│   │   └── XyzDto.kt                    (Firestore belgeden gelen ham veri)
│   ├── entity/
│   │   └── XyzEntity.kt                 (Room entity)
│   ├── mapper/
│   │   └── XyzMapper.kt                 (DTO → Domain Model dönüşümü)
│   └── repository/
│       └── XyzRepositoryImpl.kt         (Repository implementasyonu)
│
├── domain/
│   ├── model/
│   │   └── Xyz.kt                       (Domain modeli, saf Kotlin data class)
│   ├── repository/
│   │   └── XyzRepository.kt             (Repository interface)
│   └── usecase/
│       ├── GetXyzUseCase.kt
│       ├── CreateXyzUseCase.kt
│       └── ...
│
└── presentation/
    ├── viewmodel/
    │   └── XyzViewModel.kt              (StateFlow, Hilt ViewModel)
    ├── state/
    │   └── XyzUiState.kt               (Sealed class / data class)
    ├── event/
    │   └── XyzUiEvent.kt               (Kullanıcı eylemleri)
    └── screen/
        ├── XyzScreen.kt                (Compose @Composable)
        └── components/
            └── XyzCard.kt             (Yeniden kullanılabilir Compose bileşeni)
```

---

## 4. Modül Detayları

### :app

**Sorumluluk:** Uygulama giriş noktası, Navigation Compose host, Hilt bileşeni.

```
app/
├── MainActivity.kt
├── MulkoraApp.kt               (Application sınıfı, Hilt @HiltAndroidApp)
├── navigation/
│   ├── MulkoraNavHost.kt       (Ana NavHost — MainNavHost)
│   ├── AuthNavHost.kt          (Kimlik doğrulama NavHost)
│   └── Screen.kt               (Rota tanımları)
└── di/
    └── AppModule.kt
```

---

### :core-common

**Sorumluluk:** Tüm modüllerin paylaştığı yardımcı sınıflar, uzantılar, sabitler, hata yönetimi.

```
core-common/
├── result/
│   └── Result.kt               (sealed class: Success, Error, Loading)
├── extension/
│   ├── StringExt.kt
│   ├── DateExt.kt
│   └── FlowExt.kt
├── util/
│   ├── DateFormatter.kt
│   ├── ContentFilter.kt        (İçerik filtresi iş mantığı)
│   └── ExifStripper.kt         (EXIF konum temizleme)
└── constants/
    └── AppConstants.kt
```

---

### :core-ui

**Sorumluluk:** Paylaşılan Compose bileşenleri, tema dışı UI yardımcıları.

```
core-ui/
├── component/
│   ├── MulkoraButton.kt
│   ├── MulkoraTextField.kt
│   ├── MulkoraCard.kt
│   ├── MulkoraAvatar.kt
│   ├── MulkoraTopBar.kt
│   ├── MulkoraBadge.kt
│   ├── LoadingIndicator.kt
│   ├── EmptyStateView.kt
│   ├── ErrorStateView.kt
│   ├── OfflineStateView.kt
│   └── RatingBar.kt
├── modifier/
│   └── CommonModifiers.kt
└── util/
    └── UiText.kt               (String resource wrapper)
```

---

### :core-designsystem

**Sorumluluk:** Tasarım sistemi — renk paleti, tipografi, tema, ikonlar, boyutlar.

```
core-designsystem/
├── theme/
│   ├── MulkoraTheme.kt
│   ├── Color.kt                (Açık + Koyu tema renkleri)
│   ├── Typography.kt
│   └── Shape.kt
├── icon/
│   └── MulkoraIcons.kt
└── token/
    └── Spacing.kt              (8dp grid token'ları)
```

---

### :core-firebase

**Sorumluluk:** Firebase servis sınıfları, Firestore erişim katmanı, Storage yardımcıları.

```
core-firebase/
├── auth/
│   └── FirebaseAuthService.kt
├── firestore/
│   └── FirestoreService.kt     (Genel CRUD helper)
├── storage/
│   └── FirebaseStorageService.kt
├── functions/
│   └── CloudFunctionsService.kt
├── messaging/
│   └── FcmService.kt
├── analytics/
│   └── AnalyticsService.kt
└── di/
    └── FirebaseModule.kt       (Hilt modülü)
```

---

### :core-network

**Sorumluluk:** Retrofit/OkHttp (gerekirse), Google Play Developer API istemcisi, network yardımcıları.

```
core-network/
├── NetworkConnectivityObserver.kt
└── di/
    └── NetworkModule.kt
```

---

### :core-database

**Sorumluluk:** Room veritabanı, DAO'lar, yerel önbellek entity'leri.

```
core-database/
├── MulkoraDatabase.kt
├── dao/
│   ├── PostDao.kt
│   ├── NotificationDao.kt
│   └── DraftDao.kt
├── entity/
│   ├── PostEntity.kt
│   ├── NotificationEntity.kt
│   └── DraftEntity.kt
└── di/
    └── DatabaseModule.kt
```

---

### :core-testing

**Sorumluluk:** Test yardımcıları, fake implementasyonlar, test kuralları.

```
core-testing/
├── fake/
│   ├── FakeAuthRepository.kt
│   ├── FakePostRepository.kt
│   └── FakeReferenceRepository.kt
├── rule/
│   └── MainDispatcherRule.kt
└── util/
    └── TestDataFactory.kt
```

---

### :feature-auth

**Sorumluluk:** Kayıt, giriş, Google Auth, şifre sıfırlama, kullanıcı adı kontrolü.

**Ekranlar:** SCR-001 – SCR-008  
**Cloud Functions:** `createUserProfile`, `checkUsername`, `reserveUsername`  
**Firestore koleksiyonları:** `users`, `usernames`

---

### :feature-onboarding

**Sorumluluk:** İlk açılış tanıtım ekranları.

**Ekranlar:** SCR-010 – SCR-013  
**DataStore:** `onboardingCompleted` bayrağı

---

### :feature-feed

**Sorumluluk:** Ana akış, Paging 3, filtreler.

**Ekranlar:** SCR-030, SCR-031  
**Firestore koleksiyonları:** `posts` (status == "approved")  
**Paging 3:** `PostPagingSource` → Firestore cursor tabanlı sayfalama

---

### :feature-explore

**Sorumluluk:** Keşfet, kategori ve şehir bazlı gönderi listeleri.

**Ekranlar:** SCR-040 – SCR-042  
**Firestore koleksiyonları:** `posts`, `categories`

---

### :feature-post-create

**Sorumluluk:** Gönderi oluşturma, düzenleme, görsel yükleme, içerik filtresi istemcisi.

**Ekranlar:** SCR-050 – SCR-053, SCR-055  
**Cloud Functions:** `submitPost`, `updatePost`  
**Firestore koleksiyonları:** `posts`, `moderationQueue`  
**Storage:** `posts/{postId}/image`

---

### :feature-post-detail

**Sorumluluk:** Gönderi detay sayfası.

**Ekranlar:** SCR-054  
**Firestore koleksiyonları:** `posts`, `comments`, `postReactions`, `postRatings`

---

### :feature-comments

**Sorumluluk:** Yorum ve cevap sistemi.

**Ekranlar:** SCR-060 – SCR-062  
**Cloud Functions:** `addComment`, `editComment`, `deleteComment`  
**Firestore koleksiyonları:** `comments` (posts/{postId}/comments alt koleksiyonu)

---

### :feature-reactions

**Sorumluluk:** Beğeni, eksi oy, puan, kaydetme işlemleri.

**Ekranlar:** SCR-070 (puan diyaloğu)  
**Cloud Functions:** `toggleReaction`, `ratePost`  
**Firestore koleksiyonları:** `postReactions`, `postRatings`, `commentReactions`

---

### :feature-reports

**Sorumluluk:** Şikâyet oluşturma akışı.

**Ekranlar:** SCR-080 – SCR-082  
**Cloud Functions:** `createReport`  
**Firestore koleksiyonları:** `reports`

---

### :feature-references

**Sorumluluk:** Referans sistemi — talep, tanıma, yazma, cevap, itiraz.

**Ekranlar:** SCR-090 – SCR-097  
**Cloud Functions:** `initiateReference`, `respondToReferenceInvite`, `submitReference`, `submitReferenceResponse`, `submitReferenceAppeal`  
**Firestore koleksiyonları:** `references`, `referenceRelations`, `referenceResponses`, `referenceAppeals`

---

### :feature-contracts

**Sorumluluk:** Sözleşme oluşturma, taraf daveti, onay, PDF üretimi.

**Ekranlar:** SCR-100 – SCR-109  
**Cloud Functions:** `createContract`, `inviteContractParty`, `requestContractEdit`, `approveContract`, `lockContractVersion`, `generateContractPdf`  
**Firestore koleksiyonları:** `contracts`, `contractVersions`, `contractTemplates`  
**Storage:** `contracts/{contractId}/pdf`

---

### :feature-notifications

**Sorumluluk:** Bildirim merkezi, FCM token yönetimi, bildirim tercihleri.

**Ekranlar:** SCR-110, SCR-111  
**Firebase Cloud Messaging:** FCM token kaydı, topic abonelikleri  
**Firestore koleksiyonları:** `notifications`

---

### :feature-profile

**Sorumluluk:** Profil görüntüleme ve düzenleme, kaydedilen gönderiler, engelleme.

**Ekranlar:** SCR-120 – SCR-125  
**Cloud Functions:** `updateProfile`, `blockUser`, `unblockUser`  
**Firestore koleksiyonları:** `users`, `blockedUsers`  
**Storage:** `users/{userId}/avatar`

---

### :feature-settings

**Sorumluluk:** Ayarlar, tema, gizlilik, KVKK metinleri, hesap silme.

**Ekranlar:** SCR-130 – SCR-149  
**DataStore:** Tema tercihi, bildirim tercihleri  
**Cloud Functions:** `deleteAccount`, `requestDataExport`

---

### :feature-billing

**Sorumluluk:** Google Play Billing, satın alma doğrulaması, geri yükleme.

**Ekranlar:** SCR-150 – SCR-152  
**Cloud Functions:** `verifyPurchase`, `restorePurchase`, `handleRefund`  
**Firestore koleksiyonları:** `purchases`

---

### :feature-ads

**Sorumluluk:** AdMob entegrasyonu, FrequencyCapManager, reklam görünürlük yönetimi.

**Ekranlar:** (Bağımsız ekran yok — diğer ekranlara enjekte edilir)  
**DataStore:** Reklam gösterim zamanları (frekans kontrolü)  
**Firebase Remote Config:** Reklam yapılandırması, frekans limitleri

---

## 5. Hilt Bağımlılık Enjeksiyonu Yapısı

```
@HiltAndroidApp MulkoraApplication
    │
    ├── @Module FirebaseModule       (core-firebase)
    │       FirebaseAuth, FirebaseFirestore, FirebaseStorage,
    │       FirebaseFunctions, FirebaseMessaging, FirebaseAnalytics
    │
    ├── @Module DatabaseModule       (core-database)
    │       MulkoraDatabase, PostDao, NotificationDao, DraftDao
    │
    ├── @Module NetworkModule        (core-network)
    │       NetworkConnectivityObserver
    │
    ├── @Module AuthModule           (feature-auth)
    │       AuthRepository → AuthRepositoryImpl
    │
    ├── @Module PostModule           (feature-post-create, feature-post-detail)
    │       PostRepository → PostRepositoryImpl
    │
    ├── @Module ReferenceModule      (feature-references)
    │       ReferenceRepository → ReferenceRepositoryImpl
    │
    ├── @Module ContractModule       (feature-contracts)
    │       ContractRepository → ContractRepositoryImpl
    │
    ├── @Module BillingModule        (feature-billing)
    │       BillingRepository → BillingRepositoryImpl
    │       BillingClientWrapper
    │
    └── @Module AdsModule            (feature-ads)
            AdManager
            FrequencyCapManager
```

---

## 6. Build Varyantları

```
buildTypes:
    debug:
        applicationIdSuffix ".debug"
        Firebase: debug projesi
        AdMob: test reklam kimlikleri
        App Check: debug provider
        
    release:
        minifyEnabled true
        proguardFiles
        Firebase: production projesi
        AdMob: production reklam kimlikleri
        App Check: Play Integrity provider
```

---

## 7. Gradle Build Dosyası Yapısı (Kotlin DSL)

```
MülkOra/
├── build.gradle.kts              (root — plugin tanımları)
├── settings.gradle.kts           (modül listesi)
├── gradle/
│   ├── libs.versions.toml        (Version Catalog)
│   └── wrapper/
├── buildSrc/ veya convention-plugins/
│   └── AndroidLibraryConvention.kt   (modüller için ortak build mantığı)
├── app/
│   └── build.gradle.kts
├── core/
│   ├── core-common/build.gradle.kts
│   ├── core-ui/build.gradle.kts
│   └── ...
└── feature/
    ├── feature-auth/build.gradle.kts
    └── ...
```

---

*Sonraki doküman: 11-firebase-collections.md*