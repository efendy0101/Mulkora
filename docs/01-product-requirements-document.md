# Mülkora — Ayrıntılı Ürün Gereksinim Dokümanı (PRD)

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## 1. Yönetici Özeti

Mülkora, Türkiye'deki kiracılar ve ev sahiplerini aynı sosyal platformda buluşturan, güvenli ve çok kullanıcılı bir Android uygulamasıdır. Platform; deneyim paylaşımı, soru-cevap, referans sistemi ve kira sözleşmesi oluşturma gibi kiracı-ev sahibi ilişkisinin tüm kritik noktalarını kapsar. Uygulama, üretim ortamına taşınabilecek kalitede geliştirilecek; demo veya prototip niteliği taşımayacaktır.

**Slogan:** Kiracı ve ev sahibinin ortak zemini.

---

## 2. Problem Tanımı

### 2.1 Mevcut Durum

Türkiye'de kiracı-ev sahibi ilişkileri büyük ölçüde güvensizlik, bilgi eksikliği ve denetim yokluğu üzerine kuruludur. Kiracılar geçmiş ev sahipleri hakkında, ev sahipleri ise geçmiş kiracılar hakkında güvenilir bilgiye ulaşamamaktadır. Kira sözleşmeleri çoğunlukla standart dışı ve tartışmalı olmakta; sorun yaşayan taraflar deneyimlerini paylaşabileceği güvenli bir platform bulamamaktadır.

### 2.2 Çözüm

Mülkora aşağıdaki temel sorunları çözer:

- **Bilgi asimetrisi:** Kiracı ve ev sahipleri deneyimlerini platform üzerinde paylaşarak karar alma süreçlerini güçlendirir.
- **Güvensizlik:** Yönetici onaylı referans sistemi, tarafların birbirini tanımasına olanak verir.
- **Standart dışı sözleşmeler:** Uygulama içi sözleşme oluşturma ve PDF indirme özelliği, hukuki belirsizliği azaltır.
- **Denetim eksikliği:** Tüm içerikler yönetici onayından geçer; küfür ve zararlı içerik filtreleri otomatik çalışır.

---

## 3. Hedef Kitle

### Birincil Kullanıcılar

| Segment | Profil | Motivasyon |
|---|---|---|
| Kiracı | 18–45 yaş, şehir sakinleri, taşınma deneyimi olanlar | Ev sahibi hakkında bilgi edinmek, deneyim paylaşmak |
| Ev Sahibi | 30–65 yaş, tek veya çok konut sahipleri | Kiracı adayını değerlendirmek, deneyim paylaşmak |
| Kiracı + Ev Sahibi | Her iki role sahip kullanıcılar | Her iki perspektiften platformu kullanmak |

### İkincil Kullanıcılar

| Segment | Profil | Motivasyon |
|---|---|---|
| Moderatör | Platform çalışanı veya güvenilir kullanıcı | İçerik denetimi yapmak |
| Yönetici | Platform yöneticisi | Tüm platform kontrolü |

---

## 4. Ürün Kapsamı

### 4.1 MVP Kapsamında Olan Özellikler

1. **Kimlik Doğrulama:** Google girişi, e-posta/şifre kaydı ve girişi, şifre sıfırlama, e-posta doğrulama, hesap silme.
2. **Profil Sistemi:** Görünen ad, kullanıcı adı (benzersiz), rol seçimi, şehir, biyografi, profil fotoğrafı.
3. **Sosyal Platform:**
   - Ana akış (onaylı gönderiler, Paging 3 ile sayfalama)
   - Gönderi oluşturma (6 tür, 14 kategori, isteğe bağlı tek görsel, anonim paylaşım)
   - Yorum ve iç içe cevap sistemi
   - Beğeni ve eksi oy (aynı anda ikisi birlikte olamaz)
   - 1–5 yıldız puanlama (bir kullanıcı bir gönderiye bir kez)
   - İçerik kaydetme
   - Şikâyet sistemi (5 içerik türü, 13 neden)
4. **Moderasyon:** Gönderi onay akışı (8 durum), küfür ve zararlı içerik filtresi (4 risk seviyesi).
5. **Referans Sistemi:** Tanıma talebi, onay/ret, referans hazırlama, yönetici onayı, cevap, itiraz (11 durum).
6. **Kira Sözleşmesi:** Şablon seçimi, form doldurma, taraf daveti, onay, PDF üretme ve indirme.
7. **Bildirimler:** Firebase Cloud Messaging + uygulama içi bildirim merkezi (19 bildirim türü).
8. **Web Yönetim Paneli:** Next.js/React, 21 bölüm, Firebase custom claims ile yetkilendirme.
9. **Reklam:** AdMob native, banner ve sınırlı interstitial; FrequencyCapManager.
10. **Satın Alma:** Google Play Billing, `remove_ads_lifetime` ürünü, backend doğrulaması.
11. **Güvenlik:** Firestore + Storage Security Rules, App Check, Cloud Functions güvenlik katmanı.
12. **KVKK:** Veri indirme talebi, hesap ve veri silme, açık rıza yönetimi.

### 4.2 MVP Kapsamı Dışındaki Özellikler

- Özel mesajlaşma (2. sürüm)
- Kullanıcı takip sistemi (2. sürüm)
- Anket gönderileri (2. sürüm)
- Elektronik imza (3. sürüm)
- Kimlik doğrulama hizmeti (3. sürüm)
- iOS uygulaması (3. sürüm)
- Yapay zekâ destekli içerik sınıflandırma (3. sürüm)

---

## 5. İş Kuralları

### 5.1 İçerik ve Moderasyon

- BR-001: Tüm yeni gönderiler, yönetici veya moderatör onaylanana kadar genel akışta görünmez.
- BR-002: Bir gönderi 8 farklı durumdan birinde bulunabilir (taslak, inceleme bekliyor, otomatik filtreye takıldı, yönetici incelemesinde, düzenleme bekliyor, onaylandı, reddedildi, yayından kaldırıldı, silindi).
- BR-003: Küfür/zararlı içerik filtresi 4 risk seviyesiyle çalışır; kritik risk içerik yöneticiye otomatik bildirilir.
- BR-004: Yorumlar da içerik filtresinden geçirilmeli; yüksek riskli yorumlar moderasyon kuyruğuna alınmalıdır.
- BR-005: Her gönderi kategorisi ve türü yönetim panelinden güncellenebilir olmalıdır.

### 5.2 Etkileşim Kuralları

- BR-006: Bir kullanıcı aynı gönderiye hem beğeni hem eksi oy veremez.
- BR-007: Bir kullanıcı aynı gönderiye yalnızca bir kez puan verebilir (1–5 yıldız).
- BR-008: Beğeni/eksi oy/puan sayaçları istemciden doğrudan değiştirilemez; Cloud Functions veya Firestore transaction kullanılır.
- BR-009: Aynı kullanıcı aynı içeriği yalnızca bir kez şikâyet edebilir.

### 5.3 Referans Sistemi

- BR-010: Referans akışı, hedef kullanıcının "Bu kişiyi tanıyor musunuz?" sorusuna "Evet" demesiyle başlar.
- BR-011: "Hayır" cevabında referans süreci durur ve referans yayımlanmaz.
- BR-012: Referanslar yönetici onayından geçmeden profilde görünmez.
- BR-013: Hedef kullanıcı referansa cevap yazabilir; cevap da yönetici onayından geçer.
- BR-014: Hedef kullanıcı referansa itiraz edebilir; itiraz yönetim panelinde görünür.
- BR-015: Referans sistemi kontrolsüz kara liste sistemine dönüşmemelidir; tüm içerikler moderasyon altındadır.

### 5.4 Kira Sözleşmesi

- BR-016: Sözleşme yalnızca taraflar ve yetkili yöneticiler tarafından okunabilir.
- BR-017: Her iki tarafın onayından sonra sözleşme sürümü kilitlenir ve değiştirilemez.
- BR-018: PDF üretimi güvenli backend üzerinde gerçekleştirilmeli; her belgenin benzersiz numarası ve doğrulama kodu bulunmalıdır.
- BR-019: Elektronik imza ilk sürümde zorunlu değildir.

### 5.5 Kullanıcı Hesabı

- BR-020: Kullanıcı kendi hesabına yönetici veya moderatör rolü veremez.
- BR-021: Kullanıcı adı benzersizliği backend üzerinden (Cloud Functions) güvenli şekilde kontrol edilir.
- BR-022: Rol değişikliği geçmiş gönderileri, yorumları, referansları ve sözleşmeleri bozmamalıdır.
- BR-023: Hesap silindiğinde kullanıcı verileri KVKK gerekliliklerine göre silinir veya anonimleştirilir.

### 5.6 Reklam ve Satın Alma

- BR-024: Satın alma tokenı yalnızca backend üzerinden Google Play API ile doğrulanır.
- BR-025: Satın alma durumu yalnızca yerel depolamada tutulmaz; Firestore'da saklanır.
- BR-026: Uygulama yeniden kurulduğunda satın alma geri yüklenebilir olmalıdır.
- BR-027: Gönderi oluşturma, kayıt/giriş, şikâyet, referans yazma ve sözleşme düzenleme ekranlarında reklam gösterilmez.

### 5.7 Güvenlik

- BR-028: Tüm Cloud Functions'da Firebase Authentication, App Check, rol kontrolü, veri doğrulama, rate limit, idempotency ve hata yönetimi uygulanmalıdır.
- BR-029: Görsellerden EXIF konum bilgisi kaldırılmalıdır.
- BR-030: Firebase Storage yolları sahiplik ve içerik türüne göre korunmalıdır.
- BR-031: Denetim kayıtları sıradan kullanıcılar tarafından okunamaz.

---

## 6. Teknik Gereksinimler Özeti

### 6.1 Platform

- Android (minimum SDK: TBD — önerilen API 26+, hedef API 35)
- Kotlin + Jetpack Compose + Material 3

### 6.2 Backend

- Firebase Authentication
- Cloud Firestore
- Firebase Storage
- Firebase Cloud Functions (Node.js/TypeScript)
- Firebase Cloud Messaging
- Firebase App Check
- Firebase Crashlytics
- Firebase Analytics
- Firebase Remote Config

### 6.3 Yönetim Paneli

- Next.js veya React + TypeScript
- Firebase Hosting

### 6.4 Gelir Modeli

- Google AdMob (native, banner, sınırlı interstitial)
- Google Play Billing (`remove_ads_lifetime` — tüketilmeyen ürün)

---

## 7. Kalite Gereksinimleri

- Uygulama çökme oranı %0,5'in altında olmalıdır (Firebase Crashlytics ile izlenir).
- Gönderi listesi sayfalama süresi 2 saniyenin altında olmalıdır.
- Resim yükleme başarı oranı %99'un üzerinde olmalıdır.
- Firestore Security Rules, Firebase Emulator Suite ile tam olarak test edilmelidir.
- Tüm kullanıcı girişleri istemci ve sunucu tarafında doğrulanmalıdır.

---

## 8. Hukuki ve Uyum Gereksinimleri

- KVKK (Kişisel Verilerin Korunması Kanunu) uyumluluğu zorunludur.
- Gizlilik politikası, kullanım koşulları, KVKK aydınlatma metni ve topluluk kuralları ekranları bulunmalıdır.
- Referans ve sözleşme verileri hassas kişisel veri niteliği taşır; sıkı erişim kontrolüne tabidir.
- Reklam ve analiz SDK'ları için gizlilik politikasında açıklama yapılmalıdır.
- Kullanıcının verilerini indirme ve silme talebi oluşturabilmesi sağlanmalıdır.
- Firebase ve üçüncü taraf servisler için hukuki değerlendirme zorunludur.

---

## 9. Başarı Kriterleri

| Kriter | Hedef |
|---|---|
| MVP tamamlanma | Tüm Bölüm 29 özellikleri çalışır durumda |
| Güvenlik | Firestore Rules tüm test senaryolarını geçer |
| Performans | Feed yükleme < 2 sn, görsel yükleme başarısı > %99 |
| KVKK | Veri indirme ve silme akışı çalışıyor |
| Gelir | AdMob entegrasyonu + satın alma doğrulaması çalışıyor |
| Moderasyon | Tüm gönderi onay akışları çalışıyor |

---

*Sonraki doküman: 02-functional-requirements.md*