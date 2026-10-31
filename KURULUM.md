# Mülkora — Tam Kurulum Rehberi

> Bu belge; Firebase, Google AdMob, Google Play Console ve Web Admin Panel kurulum adımlarını kapsamaktadır.  
> Tüm adımları sırayla takip edin. Bir adımı atlamak sonraki adımların çalışmamasına neden olabilir.

---

## İçindekiler

1. [Ön Koşullar](#1-ön-koşullar)
2. [Firebase Projesi Kurulumu](#2-firebase-projesi-kurulumu)
3. [Firebase Authentication](#3-firebase-authentication)
4. [Cloud Firestore](#4-cloud-firestore)
5. [Firebase Storage](#5-firebase-storage)
6. [Firebase Cloud Functions](#6-firebase-cloud-functions)
7. [Firebase Cloud Messaging (FCM)](#7-firebase-cloud-messaging-fcm)
8. [Firebase Hosting — Admin Panel](#8-firebase-hosting--admin-panel)
9. [Google AdMob](#9-google-admob)
10. [Google Play Console](#10-google-play-console)
11. [Android Uygulaması Yapılandırması](#11-android-uygulaması-yapılandırması)
12. [Admin Paneli Kurulumu](#12-admin-paneli-kurulumu)
13. [Firestore İndekslerini Deploy Etme](#13-firestore-i̇ndekslerini-deploy-etme)
14. [Güvenlik Kurallarını Deploy Etme](#14-güvenlik-kurallarını-deploy-etme)
15. [Test Ortamı (Emulator)](#15-test-ortamı-emulator)
16. [Üretim Ortamına Deploy](#16-üretim-ortamına-deploy)
17. [Kontrol Listesi](#17-kontrol-listesi)

---

## 1. Ön Koşullar

### Gerekli Yazılımlar

| Yazılım | Sürüm | İndirme |
|---------|-------|---------|
| Node.js | 20 LTS+ | https://nodejs.org |
| Firebase CLI | 13+ | `npm install -g firebase-tools` |
| Android Studio | 2024.1+ | https://developer.android.com/studio |
| Java (JDK) | 17+ | https://www.oracle.com/java |
| Git | Son sürüm | https://git-scm.com |

### Gerekli Hesaplar

- Google Hesabı (Gmail)
- Google Play Console geliştirici hesabı (tek seferlik $25 ücret)
- AdMob hesabı (aynı Gmail ile)

### Firebase CLI Giriş

```bash
firebase login
```

Tarayıcıda Google hesabınızla oturum açın.

---

## 2. Firebase Projesi Kurulumu

### 2.1 İki Ayrı Firebase Projesi Oluşturun

Mülkora; **geliştirme (dev)** ve **üretim (prod)** için iki ayrı Firebase projesi kullanır.

**https://console.firebase.google.com** adresine gidin.

#### Geliştirme Projesi

1. **"Proje ekle"** → Proje adı: `mulkora-dev`
2. Google Analytics: İsteğe bağlı (geliştirme için kapatabilirsiniz)
3. **"Proje oluştur"**

#### Üretim Projesi

1. **"Proje ekle"** → Proje adı: `mulkora-prod`
2. Google Analytics: **Aktif edin** → yeni bir Analytics hesabı oluşturun
3. **"Proje oluştur"**

### 2.2 Firebase Projelerini Yerel Ortama Bağlayın

Proje dizininde:

```bash
# Geliştirme alias'ı ekle
firebase use --add
# Hangi proje? → mulkora-dev seçin
# Alias adı: dev

# Üretim alias'ı ekle
firebase use --add
# Hangi proje? → mulkora-prod seçin
# Alias adı: prod
```

Artık `firebase use dev` ve `firebase use prod` komutlarıyla ortamlar arasında geçiş yapabilirsiniz.

---

## 3. Firebase Authentication

### 3.1 Kimlik Doğrulama Yöntemlerini Etkinleştirme

Her iki proje için ayrı ayrı yapın:

1. Firebase Console → Projenizi seçin
2. Sol menü → **Authentication** → **Sign-in method** sekmesi
3. Aşağıdaki yöntemleri **Etkinleştir**:

#### E-posta/Şifre

- **E-posta/Şifre** satırına tıklayın
- "E-posta/Şifre" anahtarını açın
- **Kaydet**

#### Google

- **Google** satırına tıklayın
- "Google" anahtarını açın
- **Proje için genel adı** girin: `Mülkora`
- **Proje destek e-postası** seçin
- **Kaydet**

### 3.2 Yetkili Domain Listesi

Authentication → **Settings** → **Yetkili Domainler**:

- `localhost` (zaten var)
- `dev.example.com` (dev için)
- `mulkora.web.app` (prod için)
- Varsa özel domain: `admin.example.com`

### 3.3 E-posta Şablonları (Türkçeleştirme)

Authentication → **Templates** sekmesi:

1. **E-posta adresi doğrulama**:
   - Konu: `Mülkora - E-posta Adresinizi Doğrulayın`
   - İçeriği Türkçe olarak düzenleyin

2. **Şifre sıfırlama**:
   - Konu: `Mülkora - Şifrenizi Sıfırlayın`
   - İçeriği Türkçe olarak düzenleyin

---

## 4. Cloud Firestore

### 4.1 Firestore Veritabanı Oluşturma

1. Firebase Console → **Firestore Database**
2. **"Veritabanı oluştur"**
3. Mod: **Üretim modu** (güvenlik kuralları sonraki adımda deploy edilecek)
4. Konum: **europe-west1** (Frankfurt — Türkiye'ye en yakın)
5. **"Etkinleştir"**

> ⚠️ Konum seçildikten sonra değiştirilemez. `europe-west1` seçin.

### 4.2 Güvenlik Kurallarını Deploy Etme

```bash
firebase use dev
firebase deploy --only firestore:rules
```

Prod için:

```bash
firebase use prod
firebase deploy --only firestore:rules
```

### 4.3 Firestore İndekslerini Deploy Etme

```bash
firebase use dev
firebase deploy --only firestore:indexes
```

```bash
firebase use prod
firebase deploy --only firestore:indexes
```

> İndeks oluşturma birkaç dakika sürebilir. Console'dan durumu takip edin.

---

## 5. Firebase Storage

### 5.1 Storage Bucket Oluşturma

1. Firebase Console → **Storage**
2. **"Başlayın"**
3. Mod: **Üretim modu**
4. Konum: `europe-west1` (Firestore ile aynı seçin)
5. **"Bitti"**

### 5.2 Storage Güvenlik Kurallarını Deploy Etme

```bash
firebase use dev
firebase deploy --only storage

firebase use prod
firebase deploy --only storage
```

### 5.3 CORS Yapılandırması (Admin Panel için)

Admin panelinin Storage'a erişebilmesi için CORS ayarlanması gerekir.

`cors.json` dosyası oluşturun (proje kökünde):

```json
[
  {
    "origin": ["https://dev.example.com", "https://app.example.com", "http://localhost:3000"],
    "method": ["GET"],
    "maxAgeSeconds": 3600
  }
]
```

Uygulayın:

```bash
# Google Cloud SDK gerekir: https://cloud.google.com/sdk/docs/install
gcloud storage buckets update gs://mulkora-dev.appspot.com --cors-file=cors.json
gcloud storage buckets update gs://mulkora-prod.appspot.com --cors-file=cors.json
```

---

## 6. Firebase Cloud Functions

### 6.1 Billing Etkinleştirme (Zorunlu)

Cloud Functions kullanımı için Firebase projeniz **Blaze (Kullandıkça Öde)** planına alınmalıdır.

1. Firebase Console → Sol altta **"Planı yükselt"**
2. **Blaze planını** seçin
3. Ödeme yöntemi ekleyin (kredi kartı)
4. Ücretsiz kullanım sınırları dahilindeyken ücret alınmaz

> 💡 Ücretsiz kota: Ayda 2M çağrı, 400K GB-sn işlem. Başlangıç için yeterlidir.

### 6.2 Functions Bağımlılıklarını Yükleme

```bash
cd functions
npm install
cd ..
```

### 6.3 Functions Runtime Yapılandırması

`functions/.env.dev` dosyası oluşturun:

```env
NODE_ENV=development
```

`functions/.env.prod` dosyası oluşturun:

```env
NODE_ENV=production
```

### 6.4 TypeScript Derleme

```bash
cd functions
npm run build
cd ..
```

### 6.5 Functions Deploy Etme

```bash
# Geliştirme
firebase use dev
firebase deploy --only functions

# Üretim
firebase use prod
firebase deploy --only functions
```

> ⏱️ İlk deploy 5-10 dakika sürebilir.

### 6.6 Region Ayarı Doğrulama

Tüm Cloud Functions `europe-west1` bölgesinde deploy edilmiştir.  
`functions/src/index.ts` içinde her fonksiyon `region("europe-west1")` ile işaretlenmiştir.

### 6.7 Admin Kullanıcı Rolü Atama

İlk admin kullanıcısını oluşturmak için Firebase Console'u kullanın:

1. Authentication → İlk giriş yapıp hesap oluşturduktan sonra UID'yi kopyalayın
2. **Firebase Console → Firestore → users koleksiyonu**
3. İlgili kullanıcı belgesini açın
4. `role` alanını `"admin"` olarak güncelleyin

Veya Cloud Functions üzerinden (daha güvenli):

```bash
# Firebase Admin SDK ile
firebase functions:call setUserRole --data '{"targetUid":"USER_UID_HERE","role":"admin"}'
```

---

## 7. Firebase Cloud Messaging (FCM)

### 7.1 Android FCM Yapılandırması

FCM, Android uygulaması için `google-services.json` içinde otomatik yapılandırılır (bkz. Bölüm 11).

### 7.2 Sunucu Anahtarı (Functions için)

Cloud Functions, FCM bildirimleri gönderirken **Firebase Admin SDK**'yı kullandığından ek anahtar gerekmez. Admin SDK otomatik olarak kimlik doğrulama yapar.

### 7.3 FCM Token Test

Android uygulaması ilk açılışta FCM token'ı Firestore'a kaydeder. Firebase Console'dan doğrulayın:

- Firestore → `users/{uid}` belgesi → `fcmTokens` alanı dolu olmalı

---

## 8. Firebase Hosting — Admin Panel

### 8.1 Hosting Etkinleştirme

```bash
firebase use dev
firebase hosting:enable
```

### 8.2 Admin Panel Build

```bash
cd admin-panel
npm install
npm run build   # Next.js static export → out/ klasörü oluşur
cd ..
```

### 8.3 Hosting Deploy

```bash
firebase use dev
firebase deploy --only hosting

firebase use prod
firebase deploy --only hosting
```

Admin panel şu adreste yayınlanır:
- Dev: `https://dev.example.com`
- Prod: `https://app.example.com`

### 8.4 Özel Domain (İsteğe Bağlı)

1. Firebase Console → Hosting → **"Özel domain ekle"**
2. Domain: `admin.example.com`
3. DNS kayıtlarını domain sağlayıcınızda güncelleyin (TXT doğrulama + A kaydı)

---

## 9. Google AdMob

### 9.1 AdMob Hesabı Oluşturma

1. **https://admob.google.com** adresine gidin
2. Gmail hesabınızla giriş yapın
3. Ülke: **Türkiye**
4. Para birimi: **TRY**
5. **"Başlayın"**

### 9.2 Uygulama Ekleme

1. AdMob Console → **Uygulamalar** → **"Uygulama Ekle"**
2. Platform: **Android**
3. Uygulama Google Play'de mi? 
   - Henüz yayınlanmadıysa: **"Hayır"** → manuel ekle
   - Yayınlandıysa: **"Evet"** → paketi ara
4. Uygulama adı: `Mülkora`
5. **"Uygulamayı Ekle"**

**Uygulama ID'nizi not edin** (format: `ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX`)

### 9.3 Reklam Birimleri Oluşturma

**a) Banner Reklam:**
1. Uygulama → **"Reklam birimi oluştur"** → **"Banner"**
2. Ad: `Mulkora Banner Feed`
3. **"Reklam birimi oluştur"**
4. ID'yi not edin: `ca-app-pub-XXXX/XXXXXXXXXX`

**b) Native Advanced Reklam:**
1. **"Reklam birimi oluştur"** → **"Native Advanced"**
2. Ad: `Mulkora Native Feed`
3. **"Reklam birimi oluştur"**
4. ID'yi not edin

**c) Interstitial Reklam:**
1. **"Reklam birimi oluştur"** → **"Interstitial"**
2. Ad: `Mulkora Interstitial`
3. **"Reklam birimi oluştur"**
4. ID'yi not edin

### 9.4 Test Reklam ID'leri

Geliştirme sırasında Google'ın test ID'lerini kullanın:

```
Test App ID:          ca-app-pub-3940256099942544~3347511713
Test Banner ID:       ca-app-pub-3940256099942544/6300978111
Test Native ID:       ca-app-pub-3940256099942544/2247696110
Test Interstitial ID: ca-app-pub-3940256099942544/1033173712
```

### 9.5 local.properties Yapılandırması

`local.properties` dosyasına (proje kökünde, `.gitignore`'da) ekleyin:

```properties
# AdMob
ADMOB_APP_ID_DEBUG=ca-app-pub-3940256099942544~3347511713
ADMOB_APP_ID_RELEASE=ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX

ADMOB_BANNER_ID_DEBUG=ca-app-pub-3940256099942544/6300978111
ADMOB_BANNER_ID_RELEASE=ca-app-pub-XXXX/XXXXXXXXXX

ADMOB_NATIVE_ID_DEBUG=ca-app-pub-3940256099942544/2247696110
ADMOB_NATIVE_ID_RELEASE=ca-app-pub-XXXX/XXXXXXXXXX

ADMOB_INTERSTITIAL_ID_DEBUG=ca-app-pub-3940256099942544/1033173712
ADMOB_INTERSTITIAL_ID_RELEASE=ca-app-pub-XXXX/XXXXXXXXXX
```

---

## 10. Google Play Console

### 10.1 Geliştirici Hesabı

1. **https://play.google.com/console** adresine gidin
2. **"Şimdi Kaydolun"** → Google hesabıyla giriş
3. Geliştirici adı: `Mülkora` (veya şirket adınız)
4. Kayıt ücreti: **$25 (tek seferlik)**
5. Ödeme yapın ve hesabı doğrulayın

### 10.2 Yeni Uygulama Oluşturma

1. Play Console → **"Uygulama oluştur"**
2. Uygulama adı: `Mülkora`
3. Varsayılan dil: **Türkçe (Türkiye)**
4. Uygulama veya oyun: **Uygulama**
5. Ücretsiz mi ücretli mi: **Ücretsiz** (reklam + uygulama içi satın alma)
6. **"Oluştur"**

### 10.3 Uygulama Paket Adı

`app/build.gradle.kts` dosyasındaki `applicationId = "com.mulkora.app"` değeri Play Store'da **kalıcıdır**, değiştirilemez.

### 10.4 Google Play Billing Kurulumu

#### Subscription/Ürün Oluşturma

1. Play Console → Uygulamanız → **Ürünler → Uygulama içi ürünler**
2. **"Ürün oluştur"**
3. Ürün ID: `remove_ads` (CLAUDE.md'de belirtilen ID ile aynı olmalı)
4. Adı: `Reklamları Kaldır`
5. Fiyat: İstediğiniz fiyatı ayarlayın (ör. ₺99,99)
6. Durum: **Etkin**
7. **"Kaydet"**

#### Play Billing Library Doğrulaması

`feature/feature-billing/build.gradle.kts` dosyasında `billing.ktx` bağımlılığı zaten mevcut.

#### Gerçek Cihaz Testi

Billing işlemini test etmek için:
1. Play Console → **Dahili test** kanalına APK yükleyin
2. Test kullanıcılarını ekleyin (Lisans testi → Test hesapları)
3. Test hesaplarıyla satın alma akışını deneyin

### 10.5 Uygulama Kümeleme ve SHA-1

Android uygulamasının SHA-1 parmak izini Firebase'e eklemeniz gerekir:

```bash
# Debug imzası için
cd android
./gradlew signingReport
```

Çıktıdan `SHA1` değerini kopyalayın.

Firebase Console → Proje Ayarları → **Android Uygulaması** → **SHA sertifikası parmak izi ekle** → Yapıştırın → **Kaydet**

### 10.6 Google Play Uygulama İmzalama (App Signing)

1. Play Console → Uygulamanız → **Kurulum → Uygulama imzalama**
2. **"Uygulama imzalamayı etkinleştir"**
3. Mevcut keystore varsa yükleyin; yoksa Google yönetsin

> ⚠️ Üretim SHA-1'i de Firebase'e ekleyin (Google Play tarafından sağlanan).

---

## 11. Android Uygulaması Yapılandırması

### 11.1 google-services.json Dosyaları

Her Firebase projesi için ayrı `google-services.json` gerekir.

**Dev için:**

1. Firebase Console → `mulkora-dev` projesi → **Proje Ayarları**
2. **"Android uygulaması ekle"**
3. Paket adı: `com.mulkora.app.debug`
4. Uygulama takma adı: `Mülkora Dev`
5. SHA-1: Debug SHA-1'i ekleyin
6. **"google-services.json"** indirin
7. `app/src/debug/` klasörüne koyun (yoksa oluşturun)

**Prod için:**

1. Firebase Console → `mulkora-prod` projesi → **Proje Ayarları**
2. **"Android uygulaması ekle"**
3. Paket adı: `com.mulkora.app`
4. Uygulama takma adı: `Mülkora`
5. SHA-1: Release SHA-1'i ekleyin
6. **"google-services.json"** indirin
7. `app/src/release/` klasörüne koyun (yoksa oluşturun)

> Eğer tek dosya kullanacaksanız: `app/google-services.json` olarak koyun.

### 11.2 local.properties Dosyası

`local.properties` dosyası `.gitignore`'dadır ve repoya girmez. Her geliştirici kendi dosyasını oluşturur:

```properties
# Android SDK yolu (otomatik oluşur)
sdk.dir=C\:\\Users\\KULLANICI_ADI\\AppData\\Local\\Android\\Sdk

# Firebase
GOOGLE_SERVICES_JSON_DEBUG=app/src/debug/google-services.json
GOOGLE_SERVICES_JSON_RELEASE=app/src/release/google-services.json

# AdMob App ID (Bölüm 9'dan)
ADMOB_APP_ID_DEBUG=ca-app-pub-3940256099942544~3347511713
ADMOB_APP_ID_RELEASE=ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX
```

### 11.3 İmzalama Yapılandırması (Release Build)

Üretim APK/AAB için keystore oluşturun:

```bash
keytool -genkey -v -keystore mulkora-release.jks \
  -alias mulkora -keyalg RSA -keysize 2048 -validity 10000
```

`local.properties` dosyasına ekleyin:

```properties
KEYSTORE_FILE=../mulkora-release.jks
KEYSTORE_PASSWORD=SIFRENIZ
KEY_ALIAS=mulkora
KEY_PASSWORD=SIFRENIZ
```

`app/build.gradle.kts` içindeki `signingConfigs` bloğunu yorum satırından çıkarın.

### 11.4 İlk Derleme

```bash
# Debug build
./gradlew assembleDebug

# Release build (imzalama yapılandırıldıktan sonra)
./gradlew bundleRelease
```

---

## 12. Admin Paneli Kurulumu

### 12.1 Bağımlılıkları Yükleme

```bash
cd admin-panel
npm install
```

### 12.2 Ortam Değişkenleri

`admin-panel/.env.local` dosyası oluşturun (`.env.local.example` dosyasını referans alın):

```env
# Firebase Dev Projesi
NEXT_PUBLIC_FIREBASE_API_KEY=AIzaSy...
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=mulkora-dev.firebaseapp.com
NEXT_PUBLIC_FIREBASE_PROJECT_ID=mulkora-dev
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=mulkora-dev.appspot.com
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=123456789
NEXT_PUBLIC_FIREBASE_APP_ID=1:123456789:web:abc123
```

Firebase Console → Proje Ayarları → **"Web uygulaması ekle"** → Config'i kopyalayın.

### 12.3 Firebase Web Uygulaması Ekleme

1. Firebase Console → Proje Ayarları → **"Uygulama ekle"** → Web (</> simgesi)
2. Uygulama takma adı: `Mülkora Admin Panel`
3. Firebase Hosting: **Seçin** (bu proje için hosting'i etkinleştirin)
4. **"Uygulamayı kaydet"**
5. Gösterilen konfigürasyonu `.env.local` dosyasına yapıştırın

### 12.4 Admin Panel Yerel Başlatma

```bash
cd admin-panel
npm run dev
```

Tarayıcıda: **http://localhost:3000**

Admin hesabı ile giriş yapın (Bölüm 6.7'de oluşturduğunuz hesap).

### 12.5 Admin Panel Build ve Deploy

```bash
cd admin-panel
npm run build    # → admin-panel/out/ klasörü oluşur
cd ..

firebase use dev
firebase deploy --only hosting
```

---

## 13. Firestore İndekslerini Deploy Etme

```bash
firebase use dev
firebase deploy --only firestore:indexes

firebase use prod
firebase deploy --only firestore:indexes
```

İndeksler `firestore.indexes.json` dosyasından okunur. Oluşturulması **3-10 dakika** sürebilir. Firebase Console → Firestore → **İndeksler** sekmesinden durumu izleyin.

---

## 14. Güvenlik Kurallarını Deploy Etme

### Firestore Kuralları

```bash
firebase use dev
firebase deploy --only firestore:rules

firebase use prod
firebase deploy --only firestore:rules
```

### Storage Kuralları

```bash
firebase use dev
firebase deploy --only storage

firebase use prod
firebase deploy --only storage
```

### Kuralları Test Etme (Emulator ile)

```bash
cd tests
npm install
npm run test:rules
```

---

## 15. Test Ortamı (Emulator)

### 15.1 Firebase Emulator Suite Kurulumu

```bash
firebase setup:emulators:firestore
firebase setup:emulators:auth
firebase setup:emulators:functions
firebase setup:emulators:storage
firebase setup:emulators:hosting
```

### 15.2 Emulatorları Başlatma

```bash
# Proje kökünde
firebase emulators:start
```

Emulator UI: **http://localhost:4000**

| Servis | Port |
|--------|------|
| Auth | 9099 |
| Firestore | 8080 |
| Functions | 5001 |
| Storage | 9199 |
| Hosting | 5000 |
| UI | 4000 |

### 15.3 Android Uygulamasını Emulator'a Bağlama

`app/src/debug/` klasöründe emulator yapılandırması Android uygulamasında zaten `FirebaseModule.kt` içinde konfigüre edilmiştir.

### 15.4 Güvenlik Kuralı Testlerini Çalıştırma

```bash
# Terminal 1: Emulator
firebase emulators:start --only firestore,auth

# Terminal 2: Testler
cd tests
npm run test:rules
```

### 15.5 Cloud Functions Testlerini Çalıştırma

```bash
cd tests
npm run test:functions
```

---

## 16. Üretim Ortamına Deploy

### Tek Komutla Tam Deploy

```bash
firebase use prod

# Functions derleme
cd functions && npm run build && cd ..

# Admin panel build
cd admin-panel && npm run build && cd ..

# Tümünü deploy et
firebase deploy
```

### Bileşen Bazlı Deploy

```bash
# Sadece Functions
firebase deploy --only functions

# Sadece Hosting (Admin Panel)
firebase deploy --only hosting

# Sadece Firestore (kurallar + indeksler)
firebase deploy --only firestore

# Sadece Storage kuralları
firebase deploy --only storage
```

---

## 17. Kontrol Listesi

Canlıya geçmeden önce aşağıdakilerin tamamlandığından emin olun:

### Firebase

- [ ] `mulkora-dev` projesi oluşturuldu
- [ ] `mulkora-prod` projesi oluşturuldu
- [ ] Her iki proje için Authentication etkinleştirildi (E-posta + Google)
- [ ] E-posta şablonları Türkçe yapıldı
- [ ] Firestore oluşturuldu (europe-west1)
- [ ] Firestore güvenlik kuralları deploy edildi
- [ ] Firestore indeksleri deploy edildi ve aktif
- [ ] Storage oluşturuldu (europe-west1)
- [ ] Storage güvenlik kuralları deploy edildi
- [ ] Cloud Functions deploy edildi (49 fonksiyon)
- [ ] Firebase Hosting etkinleştirildi
- [ ] Admin panel deploy edildi
- [ ] İlk admin kullanıcısı oluşturuldu ve role="admin" atandı

### AdMob

- [ ] AdMob hesabı oluşturuldu
- [ ] Android uygulaması AdMob'a eklendi
- [ ] Banner reklam birimi oluşturuldu
- [ ] Native reklam birimi oluşturuldu
- [ ] Interstitial reklam birimi oluşturuldu
- [ ] Üretim ID'leri `local.properties`'e eklendi

### Google Play

- [ ] Play Console hesabı açıldı ($25 ödendi)
- [ ] Uygulama oluşturuldu
- [ ] `remove_ads` uygulama içi ürünü oluşturuldu ve etkinleştirildi
- [ ] Uygulama imzalama yapılandırıldı
- [ ] Release SHA-1 Firebase'e eklendi
- [ ] İç test kanalına AAB yüklendi
- [ ] Billing testi yapıldı

### Android

- [ ] `google-services.json` (debug) `app/src/debug/` klasörüne koyuldu
- [ ] `google-services.json` (release) `app/src/release/` klasörüne koyuldu
- [ ] `local.properties` oluşturuldu (AdMob ID'leri + keystore bilgileri)
- [ ] Release build başarıyla derlendi
- [ ] FCM token Firestore'a kaydediliyor (uygulama açılışında doğrulayın)

### Admin Panel

- [ ] `admin-panel/.env.local` oluşturuldu (Firebase config)
- [ ] Yerel geliştirme çalışıyor (`npm run dev`)
- [ ] `npm run build` başarılı (static export)
- [ ] Firebase Hosting'e deploy edildi
- [ ] Admin hesabı ile giriş yapılabildi

### Güvenlik

- [ ] Firestore kuralları `tests/security-rules/` ile test edildi
- [ ] Storage kuralları doğrulandı
- [ ] Admin paneli yalnızca admin/moderatör rollerine açık
- [ ] `local.properties` ve `.env.local` dosyaları `.gitignore`'da

---

## Sorun Giderme

### "Functions deployment failed"

```bash
cd functions
npm run build
# TypeScript hatalarını düzeltin
```

### "Permission denied" Firestore hatası

Güvenlik kurallarının deploy edildiğinden emin olun:

```bash
firebase deploy --only firestore:rules
```

### AdMob reklamları görünmüyor

1. Test cihazınızı test cihazı olarak AdMob'a ekleyin
2. `local.properties`'de test App ID kullandığınızdan emin olun
3. İlk reklamın görünmesi 1-2 saat sürebilir

### Google Sign-In çalışmıyor

Firebase Console → Authentication → SHA-1 parmak izinin eklendiğini doğrulayın.

---

*Son güncelleme: Temmuz 2026 | Mülkora v1.0*