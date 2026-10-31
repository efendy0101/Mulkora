# Mülkora — Kullanıcı Yolculukları

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## 1. Yeni Kullanıcı Kayıt Yolculuğu

### Senaryo: İlk kez platforma katılan bir kiracı

```
KULLANICI                    UYGULAMA                      BACKEND
   |                            |                               |
   |--- Uygulamayı aç -------> |                               |
   |                            |--- Oturum kontrolü --------> |
   |                            |<-- Oturum yok -------------- |
   |<-- Karşılama ekranı ------- |                               |
   |                            |                               |
   |--- "Kayıt Ol" seç ------> |                               |
   |<-- Kayıt formu ----------- |                               |
   |                            |                               |
   |--- Formu doldur           |                               |
   |   (ad, username, mail,    |                               |
   |    şifre, rol, şehir)     |                               |
   |--- Username yaz --------> |                               |
   |                            |--- checkUsername() ---------> |
   |                            |<-- Kullanılabilir ----------- |
   |<-- Yeşil onay işareti ----- |                               |
   |                            |                               |
   |--- Onay kutucuklarını     |                               |
   |   işaretle (Şartlar,      |                               |
   |   KVKK, 18+ vb.)         |                               |
   |--- "Kayıt Ol" butonu ---> |                               |
   |                            |--- createUser() -----------> |
   |                            |--- saveUserProfile() ------> |
   |                            |--- reserveUsername() ------> |
   |                            |--- sendEmailVerification() -> |
   |                            |<-- Başarılı ----------------- |
   |<-- "E-posta doğrulaması   |                               |
   |    gönderildi" mesajı ----- |                               |
   |--- Ana akışa geç -------> |                               |
   |<-- Feed ekranı ----------- |                               |
```

**Kritik Noktalar:**
- Kullanıcı adı mevcut olursa "Bu kullanıcı adı alınmış" hata mesajı anlık gösterilir.
- Onay kutucukları işaretlenmeden kayıt butonu aktif olmaz.
- Kayıt işlemi sırasında ağ hatası olursa kullanıcıya açıklayıcı mesaj gösterilir.

---

## 2. Google ile Giriş Yolculuğu

### Senaryo A: Yeni Google kullanıcısı

```
Karşılama Ekranı
   → "Google ile Devam Et" butonu
   → Google hesap seçimi (sistem diyaloğu)
   → Kullanıcı adı seçim ekranı (yeni kullanıcı için)
   → Rol seçim ekranı
   → Şehir seçim ekranı
   → Profil fotoğrafı (isteğe bağlı)
   → Onay kutucukları (şartlar, KVKK, 18+)
   → Ana akış ekranı
```

### Senaryo B: Kayıtlı Google kullanıcısı

```
Karşılama Ekranı
   → "Google ile Devam Et" butonu
   → Google hesap seçimi
   → Token doğrulama (otomatik)
   → Engel kontrolü (isBanned kontrol)
   → Ana akış ekranı
```

---

## 3. Gönderi Oluşturma ve Moderasyon Yolculuğu

### Senaryo: Kiracı bir sorun gönderisi paylaşıyor

```
Adım 1: Gönderi oluşturma
   Ana akış → "+" (Gönderi Oluştur) sekmesi
   → Tür seç: "Sorun"
   → Kategori seç: "Kira artışı"
   → Başlık yaz
   → Açıklama yaz
   → Şehir seç
   → İsteğe bağlı: görsel ekle
   → Anonim: kapalı
   → Yorumlara izin ver: açık
   → "Paylaş" butonuna bas

Adım 2: İçerik filtresi (otomatik, arka planda)
   → Yasaklı kelime taraması
   → Kişisel veri tespiti (TC kimlik, IBAN vb.)
   → Risk seviyesi belirleme

   Risk Düşük: uyarı göster → kullanıcı "Yine de paylaş" diyebilir
   Risk Orta:  gönderi kuyruğa alınır, kullanıcı bilgilendirilir
   Risk Yüksek: gönderi engellenir, kullanıcıya hata mesajı gösterilir
   Risk Kritik: engellenir ve yöneticiye otomatik bildirim gönderilir

Adım 3: Moderasyon kuyruğu
   → Gönderi durumu: "İnceleme bekliyor"
   → Kullanıcıya: "Gönderiniz incelemeye alındı. Onaylandığında bildirim alacaksınız."
   → Moderatöre yönetim panelinde yeni gönderi bildirim/rozet

Adım 4: Moderatör incelemesi
   → Moderatör yönetim panelini açar
   → Gönderiyi okur
   → Karar: Onayla / Reddet / Düzenleme İste

   Onaylandıysa:
     → Gönderi durumu: "Onaylandı"
     → Gönderi ana akışa düşer
     → Kullanıcıya push bildirim: "Gönderiniz yayımlandı"

   Reddedildiyse:
     → Gönderi durumu: "Reddedildi"
     → Kullanıcıya push bildirim: "Gönderiniz reddedildi — Gerekçe: [...]"

   Düzenleme istenirse:
     → Kullanıcıya bildirim: "Gönderinizde düzenleme gerekiyor"
     → Kullanıcı gönderiye döner, düzenler ve yeniden gönderir
```

---

## 4. Referans Sistemi Yolculuğu

### Senaryo: Kiracı Ayşe, eski ev sahibi Mehmet hakkında referans yazıyor

```
Adım 1: Referans talebi
   Ayşe → Mehmet'in profilini açar
   → "Referans Ver" butonuna basar
   → İlişki türü seçer: "Önceki ev sahibim"
   → Sistem Mehmet'e bildirim gönderir:
     "Ayşe [kullanıcı adı] size kiracısı olarak bir referans yazmak istiyor.
      Bu kişiyi tanıyor musunuz?"
   → Mehmet'e bildirimde: [Evet] / [Hayır] seçenekleri

Adım 2: Tanıma cevabı
   Seçenek A — Mehmet "Hayır" derse:
     → Ayşe'ye bildirim: "Mehmet sizi tanımadığını belirtti. Referans iptal edildi."
     → Süreç sona erer.

   Seçenek B — Mehmet "Evet" derse:
     → Ayşe'ye bildirim: "Mehmet sizi tanıdığını onayladı. Referansınızı yazabilirsiniz."
     → Ayşe referans ekranına yönlendirilir.

Adım 3: Referans yazımı (Ayşe)
   → Değerlendirme alanları (ev sahibi için):
     - İletişim: 4 yıldız | Olumlu | "Her zaman ulaşılabilirdi"
     - Bakım ve onarım: 3 yıldız | Tarafsız | "Yavaş ama sonunda ilgilendi"
     - Depozito süreci: 5 yıldız | Olumlu | "Tam zamanında iade etti"
     - Sözleşmeye uyum: 5 yıldız | Olumlu
     - Mahremiyete saygı: 4 yıldız | Olumlu
     - Genel: 4 yıldız | Olumlu | "Güvenilir bir ev sahibi"
   → "Gönder" butonuna basar
   → Referans yönetici moderasyon kuyruğuna gider

Adım 4: Yönetici incelemesi
   → Yönetici referansı inceler
   → Onaylandıysa: Mehmet'in profilinde yayımlanır
   → Reddedildiyse: Ayşe'ye red bildirimi gönderilir

Adım 5: Mehmet'in cevabı (isteğe bağlı)
   → Mehmet profilindeki referansı görür
   → "Cevapla" butonuna basar
   → Cevabını yazar
   → Cevap yönetici onayına gider
   → Onaylanan cevap referansın altında gösterilir

Adım 6: İtiraz (isteğe bağlı)
   → Mehmet referansı haksız bulursa "İtiraz Et" butonuna basar
   → Gerekçesini yazar
   → İtiraz yönetim panelinde görünür
   → Yönetici itirazı değerlendirir
```

---

## 5. Kira Sözleşmesi Yolculuğu

### Senaryo: Ev sahibi Ahmet, kiracı Zeynep ile sözleşme hazırlıyor

```
Adım 1: Sözleşme taslağı
   Ahmet → "Kira Sözleşmeleri" bölümü
   → "Yeni Sözleşme" butonu
   → Şablon seçimi (standart kira sözleşmesi)
   → Form doldurma:
     - Sözleşme tarihi
     - Taşınmaz adresi ve türü
     - Ev sahibi bilgileri (otomatik dolu)
     - Kiracı bilgileri → Zeynep'in kullanıcı adı aranır
     - Aylık kira, ödeme günü, yöntemi
     - Depozito
     - Başlangıç / bitiş tarihleri
     - Aidat, fatura sorumlulukları
     - Demirbaş listesi
     - Özel şartlar
   → "Taslağı Kaydet" veya "Kiracıya Gönder"

Adım 2: Kiracı daveti
   Ahmet → "Zeynep'e Gönder" butonuna basar
   Zeynep'e bildirim: "Ahmet sizinle kira sözleşmesi oluşturmak istiyor."
   → Zeynep uygulamada sözleşme detaylarını inceler

Adım 3: İnceleme ve müzakere
   Seçenek A — Zeynep "Onayla" derse:
     → Ahmet'e bildirim: "Zeynep sözleşmeyi onayladı."
     → İki taraf da onayladıktan sonra sözleşme kilitlenir (imutable)

   Seçenek B — Zeynep düzenleme isterse:
     → Zeynep "Düzenleme İste" butonuna basar
     → Düzenleme notunu yazar
     → Ahmet'e bildirim gönderilir
     → Ahmet değişikliği yapar ve tekrar gönderir
     → Döngü devam eder

Adım 4: Sözleşme kilitleme
   Her iki taraf onayladıktan sonra:
   → Cloud Functions sözleşmeye "kilitlendi" damgası atar
   → Sürüm numarası atanır
   → Hiçbir taraf değişiklik yapamaz

Adım 5: PDF üretimi
   → Cloud Functions PDF oluşturur:
     - Benzersiz belge numarası
     - Bütünlük hash değeri
     - Doğrulama kodu
     - Sayfa numaraları
   → Firebase Storage'a yüklenir
   → Her iki taraf PDF'i indirebilir veya paylaşabilir
```

---

## 6. Reklam Kaldırma Satın Alma Yolculuğu

```
Adım 1: Başlatma
   Kullanıcı → Ayarlar → "Reklamları Kaldır"
   → Satın alma açıklama ekranı gösterilir
   → "Satın Al" butonu

Adım 2: Google Play Billing
   → Google Play ödeme diyaloğu açılır
   → Kullanıcı onaylar
   → Google Play satın alma tokenı oluşturur

Adım 3: Backend doğrulaması
   → Android uygulama token'ı Cloud Functions'a gönderir
   → Cloud Functions Google Play Developer API üzerinden doğrular
   → Doğrulama başarılıysa:
     - Firestore'da kullanıcı satın alma kaydı oluşturulur
     - Custom claims güncellenmez (Firestore sorgusu yeterli)
     - Kullanıcıya "Reklamlar kaldırıldı" mesajı gösterilir

Adım 4: Sonuç
   → Tüm reklam bileşenleri gizlenir (banner, native, interstitial)
   → Uygulama yeniden kurulsa bile "Satın Almaları Geri Yükle" ile kurtarılır
```

---

## 7. Moderatör İş Akışı Yolculuğu

```
Adım 1: Moderatör yönetim panelini açar
   → Gösterge panelinde:
     - Onay bekleyen gönderi sayısı
     - Yeni şikâyet sayısı
     - İnceleme bekleyen referans sayısı

Adım 2: Gönderi moderasyonu
   → "Onay Bekleyenler" listesine gider
   → Gönderiyi okur
   → İçerik filtresinin ön değerlendirme notunu görür
   → Karar verir:
     a) Onayla → Gönderi yayımlanır, kullanıcıya bildirim gönderilir
     b) Reddet → Gerekçe girer → Kullanıcıya bildirim gönderilir
     c) Düzenleme İste → Not yazar → Kullanıcıya bildirim gönderilir
   → Her işlem auditLog'a yazılır

Adım 3: Şikâyet değerlendirme
   → "Şikâyetler" listesine gider
   → Şikâyet nedenini, içeriği ve şikâyet eden kullanıcıyı görür
   → Haklı bulursa içeriği gizler veya uyarı verir
   → Haksız bulursa şikâyeti reddeder
   → İşlem sonrası şikâyet sahibine bildirim gönderilir
```

---

## 8. Hesap Silme Yolculuğu (KVKK)

```
Adım 1: Talep başlatma
   Kullanıcı → Ayarlar → "Hesabı Sil"
   → "Bu işlem geri alınamaz" uyarı diyaloğu
   → E-posta/şifre ile yeniden kimlik doğrulaması istenir (güvenlik)
   → "Hesabımı Kalıcı Olarak Sil" butonu

Adım 2: Cloud Functions silme işlemi
   → Firebase Auth hesabı silinir
   → Kişisel veriler (profil fotoğrafı, biyografi, şehir, e-posta) silinir veya anonimleştirilir
   → Gönderilerdeki yazar alanı → "Silinmiş Kullanıcı"
   → Yorumlardaki yazar alanı → "Silinmiş Kullanıcı"
   → Referanslar: referans korunur ama yazar kimliği anonimleştirilir
   → Sözleşmeler: sözleşme korunur ama kişisel alanlar anonimleştirilir
   → Bildirimler silinir
   → Storage'daki profil fotoğrafı silinir
   → Firestore'daki kişisel doküman silinir
   → username koleksiyonundaki rezervasyon silinir (başkası kullanabilsin)

Adım 3: Sonuç
   → Kullanıcı uygulamadan çıkış yaptırılır
   → Karşılama ekranına yönlendirilir
```

---

*Sonraki doküman: 08-screen-list.md*