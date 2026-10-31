# Mülkora — Geliştirme Fazları ve Önceliklendirme

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## Genel Geliştirme Sırası

CLAUDE.md'de belirtilen kurala uygun olarak: **Altyapı → Backend → Güvenlik → Core feature → İkincil feature → UI → Test → Yayın**.

Hiçbir feature UI'ı, ilgili Cloud Functions ve Security Rules yazılmadan geliştirilemez. Her faz, önceki fazın tamamlanmasını gerektirir.

---

## Faz 0 — Proje Kurulumu (1–2 gün)

**Amaç:** Geliştirme ortamını hazırlamak.

- Android projesi oluşturma (multi-module yapı)
- Firebase projesi kurma (dev + prod ortamları)
- `google-services.json` (debug + release)
- `local.properties` yapılandırması (API anahtarları, AdMob ID'leri)
- `.gitignore` yapılandırması (gizli dosyalar dahil)
- Version Catalog (`libs.versions.toml`) oluşturma
- Convention plugins (`AndroidLibraryConvention`)
- `core-common`, `core-designsystem`, `core-ui` modüllerini iskelet olarak oluşturma
- Firebase Emulator Suite kurulumu ve test yapılandırması
- CI/CD temel pipeline kurulumu

**Çıktılar:** Derlenen boş uygulama + Emulator çalışıyor.

---

## Faz 1 — Firebase Altyapısı ve Security Rules (2–3 gün)

**Amaç:** Veri tabanı yapısını ve güvenlik kurallarını oluşturmak.

- Firestore koleksiyonları oluşturma (tüm koleksiyonlar boş belgelerle)
- `firestore.rules` yazma (tüm koleksiyonlar için)
- `firestore.indexes.json` oluşturma (IDX-001 – IDX-027)
- Firebase Storage rules yazma
- Firebase App Check yapılandırma (debug + Play Integrity)
- Security Rules testlerini yazma ve geçirme (TEST-RULES-001 – TEST-RULES-020)
- `bannedTerms` koleksiyonunu başlangıç verileriyle doldurma
- `categories` koleksiyonunu doldurma
- `contractTemplates` koleksiyonunu doldurma
- `appSettings` ve `adSettings` belgelerini oluşturma

**Çıktılar:** Tüm Security Rules testleri geçiyor + İndeksler aktif.

---

## Faz 2 — Authentication (Cloud Functions + Android) (3–4 gün)

**Amaç:** Kullanıcı kaydı, girişi ve hesap yönetimini tamamlamak.

**Cloud Functions:**
- `checkUsername` (CF-AUTH-002)
- `createUserProfile` (CF-AUTH-001)
- `deleteAccount` (CF-AUTH-003)

**Android (:feature-auth):**
- `AuthRepository` interface + `AuthRepositoryImpl`
- `AuthViewModel`
- SCR-001 Splash ekranı (oturum kontrolü + yönlendirme)
- SCR-002 Giriş ekranı (e-posta/şifre + Google)
- SCR-003 Kayıt ekranı (username anlık kontrol dahil)
- SCR-004 Kullanıcı adı seçimi (Google akışı)
- SCR-005 Rol seçimi
- SCR-006 Onay metinleri
- SCR-007 E-posta doğrulama bilgi
- SCR-008 Şifremi unuttum
- Google Sign-In entegrasyonu
- FCM token kaydı (`FcmTokenManager`)

**Testler:**
- `AuthViewModelTest`
- `createUserProfile` Cloud Function entegrasyon testi
- Auth akışı E2E testi

**Çıktılar:** Kullanıcı kayıt olup giriş yapabiliyor + Token Firestore'a kaydediliyor.

---

## Faz 3 — Profil Yönetimi (2–3 gün)

**Amaç:** Profil görüntüleme, düzenleme, engelleme.

**Cloud Functions:**
- `updateProfile`
- `blockUser` / `unblockUser`

**Android (:feature-profile):**
- `ProfileRepository` + `ProfileRepositoryImpl`
- SCR-120 Kendi profil ekranı
- SCR-121 Başka kullanıcı profil ekranı
- SCR-122 Profil düzenleme
- SCR-123 Profil fotoğrafı seçme + EXIF temizleme (`ExifStripper`)
- SCR-125 Engellenen kullanıcılar

**Testler:**
- `ExifStripper` unit testi (konum temizlendi mi?)
- Profile Security Rules testi

**Çıktılar:** Kullanıcı profilini düzenleyebiliyor + Fotoğraf yükleyebiliyor.

---

## Faz 4 — Gönderi Oluşturma ve Ana Akış (5–7 gün)

**Amaç:** Platform'un temel özelliğini (gönderi paylaşma ve görüntüleme) tamamlamak.

**Cloud Functions:**
- `contentFilter` yardımcı fonksiyonu
- `submitPost` (CF-POST-001)
- `updatePost`
- `moderatePost` (CF-POST-002)

**Android (:feature-post-create, :feature-post-detail, :feature-feed):**
- `PostRepository` + `PostRepositoryImpl`
- `PostPagingSource` (Firestore cursor tabanlı)
- SCR-030 Ana akış (Paging 3 + infinite scroll)
- SCR-031 Filtre bottom sheet
- SCR-050 Gönderi oluşturma
- SCR-051 Görsel seçici + SCR-052 Görsel kırpma
- SCR-053 Gönderi önizleme
- SCR-054 Gönderi detayı
- SCR-055 Gönderi düzenleme (edit_requested durumu için)

**Testler:**
- `ContentFilter` unit testleri (tüm kategoriler)
- `submitPost` entegrasyon testi (tüm risk seviyeleri)
- `PostStateMachine` unit testi
- Security Rules: gönderi okuma/yazma

**Çıktılar:** Kullanıcı gönderi paylaşabiliyor + Moderasyon kuyruğu çalışıyor.

---

## Faz 5 — Yorumlar ve Etkileşimler (3–4 gün)

**Amaç:** Yorum, cevap, beğeni, puan ve kaydetme.

**Cloud Functions:**
- `addComment` / `editComment` / `deleteComment`
- `toggleReaction` (CF-REACT-001) — Firestore transaction
- `ratePost` (CF-REACT-002) — Firestore transaction

**Android (:feature-comments, :feature-reactions):**
- `CommentRepository` + `ReactionRepository`
- SCR-060 Yorum listesi
- SCR-061 Yorum yazma/düzenleme
- SCR-062 Cevap yazma
- SCR-070 Puan verme diyaloğu
- Kaydetme butonu (savedPosts — istemci yazabilir)

**Testler:**
- `ToggleReactionUseCase` unit testi
- `ratePost` transaction testi (race condition)
- Comment Security Rules

**Çıktılar:** Gönderi detayında tam etkileşim mümkün.

---

## Faz 6 — Şikâyet Sistemi (2 gün)

**Amaç:** İçerik şikâyet akışını tamamlamak.

**Cloud Functions:**
- `createReport` (CF-REPORT-001)

**Android (:feature-reports):**
- `ReportRepository`
- SCR-080 Şikâyet nedeni
- SCR-081 Şikâyet detayı
- SCR-082 Şikâyet onayı

**Testler:**
- `createReport` entegrasyon testi (tekrar şikâyet koruması)

---

## Faz 7 — Bildirim Sistemi (2–3 gün)

**Amaç:** FCM push bildirimleri ve uygulama içi bildirim merkezi.

**Cloud Functions:**
- `sendNotification` yardımcı fonksiyonu
- Firestore triggerları: `onPostStatusChange`, `onReferenceStatusChange`

**Android (:feature-notifications):**
- `NotificationRepository`
- `MulkoraFirebaseMessagingService`
- `NotificationChannelManager`
- SCR-110 Bildirim merkezi
- SCR-111 Bildirim detayı + deep link
- Bildirim tercihleri arayüzü (Faz 10 ayarlarla birleşecek)

**Testler:**
- `NotificationViewModel` unit testi
- FCM token geçersiz olunca temizleme testi

---

## Faz 8 — Referans Sistemi (5–7 gün)

**Amaç:** Referans sistemi tüm akışlarıyla tamamlamak.

**Cloud Functions:**
- `initiateReference` (CF-REF-001)
- `respondToReferenceInvite` (CF-REF-002)
- `submitReference` (CF-REF-003)
- `moderateReference`
- `submitReferenceResponse`
- `submitReferenceAppeal`

**Android (:feature-references):**
- `ReferenceRepository`
- SCR-090 Referans listesi
- SCR-091 Referans başlatma
- SCR-092 Tanıma cevabı
- SCR-093 Referans yazma
- SCR-094 Referans detay
- SCR-095 Referans cevabı
- SCR-096 İtiraz ekranı
- SCR-097 İtiraz detay

**Testler:**
- `ReferenceStateMachine` unit testi (tüm geçişler)
- `initiateReference` entegrasyon testi (tekrar referans koruması)
- `respondToReferenceInvite` entegrasyon testi (yanlış kullanıcı)
- Referans Security Rules (onaylı/bekleyen erişim)

**Çıktılar:** Tam referans döngüsü çalışıyor.

---

## Faz 9 — Kira Sözleşmesi (5–6 gün)

**Amaç:** Sözleşme oluşturma, onaylama ve PDF üretimini tamamlamak.

**Cloud Functions:**
- `createContract` (CF-CONTRACT-001)
- `inviteContractParty`
- `requestContractEdit`
- `approveContract`
- `lockContractVersion` (CF-CONTRACT-002)
- `generateContractPdf` (CF-CONTRACT-003) — pdfkit

**Android (:feature-contracts):**
- `ContractRepository`
- SCR-100 Sözleşme listesi
- SCR-101 Şablon seçimi
- SCR-102 – SCR-105 Sözleşme formu
- SCR-106 Sözleşme önizleme
- SCR-107 Sözleşme detay
- SCR-108 Düzenleme talebi
- SCR-109 PDF indirme (Storage Signed URL)

**Testler:**
- `lockContractVersion` testi (her iki taraf onaylamadan kilitlenemiyor)
- `generateContractPdf` testi (hash değeri, doğrulama kodu)
- Sözleşme Security Rules (yalnızca taraflar okuyabilir)

**Çıktılar:** İki kullanıcı birlikte sözleşme oluşturabilir + PDF indirebilir.

---

## Faz 10 — Ayarlar, KVKK ve Hesap Yönetimi (3–4 gün)

**Amaç:** Ayarlar, gizlilik ve hesap silme akışları.

**Cloud Functions:**
- `requestDataExport` (KVKK)

**Android (:feature-settings):**
- SCR-130 Ayarlar ana ekranı
- SCR-131 Hesap ayarları + SCR-132 Şifre değiştirme
- SCR-133 Gizlilik ayarları + SCR-134 Bildirim ayarları
- SCR-135 Tema seçimi
- SCR-136 – SCR-147 Bilgilendirme metinleri (KVKK, ToS vb.)
- SCR-148 Veri indirme talebi
- SCR-149 Hesap silme (kimlik doğrulama + silinme akışı)
- Tema sistemi (Açık/Koyu/Sistem — DataStore ile saklanır)

**Testler:**
- `deleteAccount` testi (kişisel veriler temizlendi mi?)
- Hesap silme sonrası erişim testi

---

## Faz 11 — Keşfet ve Arama (2–3 gün)

**Amaç:** Keşfet ekranı ve filtreleme.

**Android (:feature-explore):**
- SCR-040 Keşfet ana ekranı
- SCR-041 Kategori gönderileri
- SCR-042 Şehir bazlı gönderiler

**Testler:**
- Keşfet filtreleme unit testi

---

## Faz 12 — Reklam ve Satın Alma (3–4 gün)

**Amaç:** AdMob entegrasyonu ve Google Play Billing.

**Cloud Functions:**
- `verifyPurchase` (CF-BILLING-001)
- `restorePurchase`
- `handleRefund` (webhook)

**Android (:feature-ads, :feature-billing):**
- `AdManager` + `FrequencyCapManager`
- Banner, Native, Interstitial reklamlar
- `BillingClientWrapper`
- SCR-150 – SCR-152 Satın alma ekranları
- Remote Config entegrasyonu

**Testler:**
- `FrequencyCapManager` unit testi (cooldown + günlük limit)
- `verifyPurchase` testi (replay attack koruması)
- `AdManager`: adsRemoved = true olduğunda reklam gösterilmiyor mu?

---

## Faz 13 — Onboarding ve Son Dokunuşlar (2 gün)

**Amaç:** İlk açılış deneyimi ve hata/boş durum ekranları.

**Android (:feature-onboarding, :core-ui):**
- SCR-010 – SCR-013 Onboarding ekranları
- `EmptyStateView`, `ErrorStateView`, `OfflineStateView`, shimmer
- SCR-160 – SCR-164 Hata ekranları
- Çevrimdışı durumu yönetimi (`NetworkConnectivityObserver`)

---

## Faz 14 — Moderatör Yönetim Paneli (3–4 gün)

**Amaç:** Moderatörlerin iş akışını web veya Android yönetim görünümü.

**Seçenek A (Web — Firebase Hosting):** Basit React admin paneli.  
**Seçenek B (Android):** Moderatör rolündeyken ayarlar veya özel bir ekran.

Minimum özellikler:
- Moderasyon kuyruğu listesi ve onay/red işlemi
- Şikâyet listesi ve değerlendirme
- Referans onay kuyruğu
- Kullanıcı engelleme

---

## Faz 15 — Performans Optimizasyonu ve Release Hazırlığı (2–3 gün)

**Amaç:** Play Store yayınına hazırlık.

- ProGuard/R8 kurallarını yapılandırma
- Firebase Crashlytics entegrasyonu
- Firebase Performance Monitoring
- Uygulama ikonu, splash screen
- Play Store listing (açıklama, ekran görüntüleri)
- Privacy policy URL
- Minimum SDK kontrolü (API 26+)
- Release keystore yönetimi
- App Bundle (AAB) oluşturma

---

## Kapsam Dışı (v1.0)

Aşağıdaki özellikler MVP kapsamında değildir; sonraki sürümlere bırakılır:

- Doğrulanmış hesap rozeti (v2)
- Mesajlaşma / Sohbet özelliği (v2)
- Konut fiyat endeksi verileri (v2)
- Kullanıcı istatistikleri dashboard (v2)
- Gönderi paylaşımı (sosyal medya) (v2)
- Web uygulaması (v3)

---

## Faz Özeti

| Faz | Konu | Süre (tahmini) |
|---|---|---|
| 0 | Proje Kurulumu | 1–2 gün |
| 1 | Firebase Altyapı + Security Rules | 2–3 gün |
| 2 | Authentication | 3–4 gün |
| 3 | Profil | 2–3 gün |
| 4 | Gönderi + Ana Akış | 5–7 gün |
| 5 | Yorumlar + Etkileşimler | 3–4 gün |
| 6 | Şikâyet | 2 gün |
| 7 | Bildirimler | 2–3 gün |
| 8 | Referans Sistemi | 5–7 gün |
| 9 | Kira Sözleşmesi | 5–6 gün |
| 10 | Ayarlar + KVKK | 3–4 gün |
| 11 | Keşfet | 2–3 gün |
| 12 | Reklam + Satın Alma | 3–4 gün |
| 13 | Onboarding + Son Dokunuşlar | 2 gün |
| 14 | Moderatör Paneli | 3–4 gün |
| 15 | Release Hazırlığı | 2–3 gün |
| **TOPLAM** | | **~45–60 gün** |

---

*Bu doküman, analiz aşamasının son belgesidir.*  
*Geliştirme aşamasına CLAUDE.md'deki kurallara uygun olarak başlanabilir.*