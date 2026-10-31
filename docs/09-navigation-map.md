# Mülkora — Navigasyon Haritası

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## 1. Uygulama Giriş Noktaları

```
Uygulama Açılışı (SCR-001 Splash)
    │
    ├── Oturum Var + Engellenmemiş
    │       └──→ Ana Akış (SCR-020)
    │
    ├── Oturum Var + Engellendi
    │       └──→ Hesap Engellendi Ekranı
    │
    └── Oturum Yok
            └──→ Karşılama / Giriş Ekranı (SCR-002)
```

---

## 2. Kimlik Doğrulama Akışı

```
SCR-002 Giriş Ekranı
    │
    ├── "Kayıt Ol" → SCR-003 Kayıt Ekranı
    │       │
    │       ├── Başarılı Kayıt
    │       │       └──→ SCR-007 E-posta Doğrulama Bilgi
    │       │               └──→ SCR-020 Ana Akış
    │       │
    │       └── Kullanıcı Adı Kontrolü (inline, anlık)
    │
    ├── "Google ile Giriş" (Yeni Kullanıcı)
    │       └──→ SCR-004 Kullanıcı Adı Seçimi
    │               └──→ SCR-005 Rol Seçimi
    │                       └──→ SCR-006 Onay Metinleri
    │                               └──→ SCR-020 Ana Akış
    │
    ├── "Google ile Giriş" (Mevcut Kullanıcı)
    │       └──→ SCR-020 Ana Akış
    │
    ├── "Şifremi Unuttum" → SCR-008 Şifre Sıfırlama
    │       └──→ "E-posta Gönderildi" bilgisi → SCR-002 Giriş
    │
    └── E-posta/Şifre Giriş Başarılı
            └──→ SCR-020 Ana Akış
```

---

## 3. Ana Navigasyon Yapısı (Bottom Navigation Bar)

```
┌──────────────────────────────────────────────────┐
│                                                  │
│    [Feed]  [Keşfet]  [+]  [Bildirim]  [Profil]  │
│                                                  │
└──────────────────────────────────────────────────┘
   SCR-020   SCR-021  SCR-022  SCR-023   SCR-024
```

Her sekme kendi back stack'ini korur. "+" (Gönderi Oluştur) bir bottom sheet veya yeni akış olarak açılır; back stack'e eklenmez.

---

## 4. Ana Akış Navigasyonu

```
SCR-020 Ana Akış
    │
    ├── Gönderi Kartına Tıkla → SCR-054 Gönderi Detay
    │       │
    │       ├── Yoruma Tıkla → SCR-060 Yorum Listesi
    │       │       ├── Cevap Yaz → SCR-062 Cevap Yazma (inline)
    │       │       └── Yorum Düzenle → SCR-061 Yorum Düzenleme
    │       │
    │       ├── Puan Ver → SCR-070 Puan Diyaloğu (bottom sheet)
    │       │
    │       ├── Şikâyet Et → SCR-080 Şikâyet Nedeni
    │       │       └──→ SCR-081 Şikâyet Detay
    │       │               └──→ SCR-082 Şikâyet Onay
    │       │
    │       └── Kullanıcı Adına Tıkla → SCR-121 Kullanıcı Profil
    │
    ├── Filtre → SCR-031 Filtre Bottom Sheet
    │
    └── Kullanıcı Adına Tıkla → SCR-121 Kullanıcı Profil
```

---

## 5. Keşfet Navigasyonu

```
SCR-021 Keşfet Ana Ekran
    │
    ├── Kategoriye Tıkla → SCR-041 Kategori Gönderileri
    │       └── (Gönderi Detay akışı — yukarıdaki gibi)
    │
    └── Şehir Seç → SCR-042 Şehir Bazlı Gönderiler
            └── (Gönderi Detay akışı — yukarıdaki gibi)
```

---

## 6. Gönderi Oluşturma Navigasyonu

```
SCR-022 Gönderi Oluştur (Bottom Sheet veya Full Screen)
    │
    ├── Görsel Ekle → SCR-051 Görsel Seçici
    │       └──→ SCR-052 Görsel Kırpma
    │               └──→ SCR-022 (görsel seçildi)
    │
    ├── Önizle → SCR-053 Gönderi Önizleme
    │       ├── "Düzenle" → SCR-022
    │       └── "Paylaş" → İçerik Filtresi → Moderasyon → SCR-020
    │
    └── "Paylaş" (doğrudan) → İçerik Filtresi → Moderasyon → SCR-020
```

---

## 7. Bildirimler Navigasyonu

```
SCR-023 Bildirim Merkezi
    │
    ├── Gönderi Bildirimi → SCR-054 Gönderi Detay
    ├── Yorum Bildirimi → SCR-054 Gönderi Detay (yoruma scroll)
    ├── Referans Bildirimi → SCR-092 Tanıma Cevabı Ekranı
    │                     veya SCR-094 Referans Detay
    ├── Sözleşme Bildirimi → SCR-107 Sözleşme Detay
    ├── PDF Bildirimi → SCR-109 PDF İndirme Ekranı
    └── Şikâyet Sonucu → SCR-082 Şikâyet Onay (bilgi amaçlı)
```

---

## 8. Profil Navigasyonu

```
SCR-024 Profil (kendi)
    │
    ├── Profil Düzenle → SCR-122 Profil Düzenleme
    │       └── Fotoğraf Değiştir → SCR-123 Fotoğraf Seçici
    │
    ├── Gönderilerim → (Gönderi Detay akışı)
    ├── Kaydedilen Gönderiler → SCR-124
    │       └── (Gönderi Detay akışı)
    │
    ├── Referanslarım → SCR-090 Referans Listesi
    │       ├── Referans Detay → SCR-094
    │       │       ├── Cevapla → SCR-095 Cevap Yazma
    │       │       └── İtiraz → SCR-096 İtiraz Ekranı
    │       │               └──→ SCR-097 İtiraz Detay
    │       │
    │       └── Yeni Referans → SCR-091 Referans Başlatma
    │               └──→ SCR-093 Referans Yazma
    │
    ├── Kira Sözleşmelerim → SCR-100 Sözleşme Listesi
    │       ├── Sözleşme Detay → SCR-107
    │       │       └── PDF İndir → SCR-109
    │       └── Yeni Sözleşme → SCR-101 Şablon Seçimi
    │               → SCR-102 → SCR-103 → SCR-104 → SCR-105
    │               → SCR-106 Önizleme → SCR-107 Detay
    │
    ├── Engellenen Kullanıcılar → SCR-125
    │
    └── Ayarlar → SCR-130 Ayarlar Ana Ekranı

SCR-121 Başka Kullanıcı Profili
    │
    ├── Gönderilerine Bak → (Gönderi Detay akışı)
    ├── Referans Ver → SCR-091 Referans Başlatma
    ├── Şikâyet Et → SCR-080 Şikâyet Nedeni
    └── Engelle → Onay Diyaloğu → (Engellendi bilgisi)
```

---

## 9. Ayarlar Navigasyonu

```
SCR-130 Ayarlar Ana Ekranı
    │
    ├── Hesap → SCR-131
    │       ├── Şifre Değiştir → SCR-132
    │       ├── Oturumu Kapat → SCR-002 Giriş Ekranı
    │       └── Hesabı Sil → SCR-149 Hesap Silme
    │               └── Kimlik Doğrulama → Cloud Functions → SCR-002
    │
    ├── Gizlilik → SCR-133
    │       └── Veri İndir → SCR-148
    │
    ├── Bildirimler → SCR-134
    │
    ├── Görünüm → SCR-135 Tema
    │
    ├── Reklamları Kaldır → SCR-150 Satın Alma
    │       ├── Google Play Billing → SCR-151 Başarı
    │       └── Geri Yükle → SCR-152 Geri Yükleme
    │
    └── Bilgilendirme
            ├── Hakkında → SCR-136
            ├── Kullanım Koşulları → SCR-137
            ├── Gizlilik Politikası → SCR-138
            ├── KVKK → SCR-139
            ├── Açık Rıza → SCR-140
            ├── Topluluk Kuralları → SCR-141
            ├── Referans Kuralları → SCR-142
            ├── Sözleşme Koşulları → SCR-143
            ├── Çerez / Üçüncü Taraf → SCR-144
            ├── Yardım ve Destek → SCR-145
            ├── SSS → SCR-146
            └── Açık Kaynak Lisansları → SCR-147
```

---

## 10. Deep Link Yapısı

Uygulama, Firebase Dynamic Links veya Android App Links üzerinden derin bağlantıları desteklemelidir.

| Deep Link Deseni | Hedef Ekran |
|---|---|
| `mulkora://post/{postId}` | SCR-054 Gönderi Detay |
| `mulkora://profile/{userId}` | SCR-121 Kullanıcı Profil |
| `mulkora://contract/{contractId}` | SCR-107 Sözleşme Detay |
| `mulkora://reference/{referenceId}` | SCR-094 Referans Detay |
| `mulkora://notification/{notifId}` | İlgili ekran |

Deep link erişimde önce oturum kontrolü yapılır; oturum yoksa giriş ekranına yönlendirilir, giriş sonrası hedef ekrana devam edilir.

---

## 11. Navigation Compose Yapısı

```kotlin
// Rota tanımları (genel yapı)
sealed class Screen(val route: String) {
    // Auth
    object Splash        : Screen("splash")
    object Login         : Screen("login")
    object Register      : Screen("register")
    object UsernameSetup : Screen("username_setup")
    object RoleSetup     : Screen("role_setup")
    object TermsConsent  : Screen("terms_consent")

    // Main (Bottom Nav)
    object Feed          : Screen("feed")
    object Explore       : Screen("explore")
    object CreatePost    : Screen("create_post")
    object Notifications : Screen("notifications")
    object Profile       : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }

    // Post
    object PostDetail    : Screen("post/{postId}") {
        fun createRoute(postId: String) = "post/$postId"
    }
    object PostEdit      : Screen("post/{postId}/edit") {
        fun createRoute(postId: String) = "post/$postId/edit"
    }

    // Reference
    object References    : Screen("references")
    object ReferenceDetail : Screen("reference/{referenceId}") {
        fun createRoute(referenceId: String) = "reference/$referenceId"
    }
    object ReferenceWrite  : Screen("reference/write/{targetUserId}") {
        fun createRoute(targetUserId: String) = "reference/write/$targetUserId"
    }

    // Contract
    object Contracts     : Screen("contracts")
    object ContractDetail : Screen("contract/{contractId}") {
        fun createRoute(contractId: String) = "contract/$contractId"
    }
    object ContractCreate : Screen("contract/create")

    // Settings
    object Settings      : Screen("settings")
    object AccountSettings : Screen("settings/account")
    object PrivacySettings : Screen("settings/privacy")
    object NotificationSettings : Screen("settings/notifications")
    object DeleteAccount : Screen("settings/delete_account")

    // Billing
    object RemoveAds     : Screen("billing/remove_ads")
}
```

**Not:** Navigation Compose yapısı iki NavHost'tan oluşur:
- `AuthNavHost`: Giriş yapılmamış kullanıcılar için (Splash, Login, Register, Onboarding).
- `MainNavHost`: Giriş yapılmış kullanıcılar için (Bottom Nav + tüm feature ekranları).

---

## 12. Geri Navigasyon Davranışları

| Durum | Beklenen Davranış |
|---|---|
| Bottom Nav sekmeleri arası | Back stack temizlenerek önceki sekmeye dönüş |
| Gönderi Detay → Geri | Önceki ekrana (akış, profil, arama) |
| Gönderi Oluşturma → Geri | Kaydedilmemiş değişiklik varsa onay diyaloğu |
| Sözleşme Formu → Geri | Kaydedilmemiş değişiklik varsa onay diyaloğu |
| Hesap Silme → Geri | Silme iptal edilir |
| Deep link → Geri | Auth akışı tamamlandıktan sonra hedef ekrandan ana akışa |

---

*Sonraki doküman: 10-android-module-structure.md*