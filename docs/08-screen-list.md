# Mülkora — Ekran Listesi

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## Kapsam

Bu liste Android uygulamasındaki tüm ekranları içerir. Her ekran için benzersiz bir ID, adı, erişim gereksinimleri ve bağlı olduğu feature modülü belirtilmiştir.

**Erişim:** Herkese Açık / Giriş Gerekli / Moderatör+ / Yönetici

---

## 1. Kimlik Doğrulama Akışı (feature-auth)

| ID | Ekran Adı | Erişim | Notlar |
|---|---|---|---|
| SCR-001 | Karşılama / Splash Ekranı | Herkese Açık | Oturum kontrolü yapılır, yönlendirme sağlanır |
| SCR-002 | Giriş Ekranı | Herkese Açık | E-posta/şifre + Google girişi |
| SCR-003 | Kayıt Ekranı | Herkese Açık | Ad, kullanıcı adı, e-posta, şifre, rol, şehir |
| SCR-004 | Kullanıcı Adı Seçimi | Herkese Açık | Google ile yeni kayıtta gösterilir |
| SCR-005 | Rol Seçimi | Herkese Açık | Kiracı / Ev Sahibi / Her İkisi |
| SCR-006 | Onay Metinleri Ekranı | Herkese Açık | Şartlar, KVKK, 18+ onay kutucukları |
| SCR-007 | E-posta Doğrulama Bilgi Ekranı | Herkese Açık | "Doğrulama e-postası gönderildi" |
| SCR-008 | Şifremi Unuttum Ekranı | Herkese Açık | E-posta girişi, sıfırlama bağlantısı gönderimi |

---

## 2. Onboarding (feature-onboarding)

| ID | Ekran Adı | Erişim | Notlar |
|---|---|---|---|
| SCR-010 | Onboarding 1 — Platformu Tanı | Herkese Açık | Swipe ile geçilebilir |
| SCR-011 | Onboarding 2 — Gönderi Paylaş | Herkese Açık | |
| SCR-012 | Onboarding 3 — Referans Sistemi | Herkese Açık | |
| SCR-013 | Onboarding 4 — Sözleşme Oluştur | Herkese Açık | |

---

## 3. Ana Navigasyon (app modülü)

Alt navigasyon sekmeleri:

| ID | Ekran Adı | Sekme | Erişim |
|---|---|---|---|
| SCR-020 | Ana Akış (Feed) | 1. Sekme | Giriş Gerekli |
| SCR-021 | Keşfet | 2. Sekme | Giriş Gerekli |
| SCR-022 | Gönderi Oluştur | 3. Sekme | Giriş Gerekli |
| SCR-023 | Bildirimler | 4. Sekme | Giriş Gerekli |
| SCR-024 | Profil | 5. Sekme | Giriş Gerekli |

---

## 4. Ana Akış (feature-feed)

| ID | Ekran Adı | Erişim | Notlar |
|---|---|---|---|
| SCR-030 | Ana Akış Listesi | Giriş Gerekli | Paging 3, infinite scroll, filtreler |
| SCR-031 | Filtre / Sıralama Bottom Sheet | Giriş Gerekli | En yeni, en çok beğenilen vb. |

---

## 5. Keşfet (feature-explore)

| ID | Ekran Adı | Erişim | Notlar |
|---|---|---|---|
| SCR-040 | Keşfet Ana Ekranı | Giriş Gerekli | Kategoriler, öne çıkan gönderiler |
| SCR-041 | Kategori Gönderileri Listesi | Giriş Gerekli | Seçili kategori gönderileri |
| SCR-042 | Şehir Bazlı Gönderi Listesi | Giriş Gerekli | |

---

## 6. Gönderi (feature-post-create, feature-post-detail)

| ID | Ekran Adı | Erişim | Notlar |
|---|---|---|---|
| SCR-050 | Gönderi Oluşturma Ekranı | Giriş Gerekli | Tür, kategori, başlık, metin, görsel, anonim |
| SCR-051 | Görsel Seçici | Giriş Gerekli | Galeri/kamera erişimi |
| SCR-052 | Görsel Kırpma Ekranı | Giriş Gerekli | |
| SCR-053 | Gönderi Önizleme | Giriş Gerekli | Paylaşmadan önce önizleme |
| SCR-054 | Gönderi Detay Ekranı | Giriş Gerekli | Gönderi içeriği + yorumlar |
| SCR-055 | Gönderi Düzenleme Ekranı | Giriş Gerekli | Yalnızca kendi gönderisini, onaydan önce |

---

## 7. Yorumlar (feature-comments)

| ID | Ekran Adı | Erişim | Notlar |
|---|---|---|---|
| SCR-060 | Yorum Listesi | Giriş Gerekli | Gönderi detayına gömülü veya ayrı sayfa |
| SCR-061 | Yorum Yazma / Düzenleme | Giriş Gerekli | Bottom sheet veya inline alan |
| SCR-062 | Cevap Yazma | Giriş Gerekli | Yoruma cevap |

---

## 8. Etkileşimler (feature-reactions)

Etkileşim bileşenleri genellikle gönderi ve yorum kartlarına gömülüdür; ayrı ekran gerektirmez. Yalnızca puanlama diyaloğu ayrıdır.

| ID | Ekran Adı | Erişim | Notlar |
|---|---|---|---|
| SCR-070 | Puan Verme Diyaloğu | Giriş Gerekli | 1–5 yıldız, bottom sheet |

---

## 9. Şikâyet (feature-reports)

| ID | Ekran Adı | Erişim | Notlar |
|---|---|---|---|
| SCR-080 | Şikâyet Nedeni Seçimi | Giriş Gerekli | Bottom sheet, 13 neden |
| SCR-081 | Şikâyet Detayı (isteğe bağlı açıklama) | Giriş Gerekli | |
| SCR-082 | Şikâyet Onay / Teşekkür Ekranı | Giriş Gerekli | |

---

## 10. Referans Sistemi (feature-references)

| ID | Ekran Adı | Erişim | Notlar |
|---|---|---|---|
| SCR-090 | Referans Listesi (kendi) | Giriş Gerekli | Verdiğim + Aldığım + Bekleyen |
| SCR-091 | Referans Başlatma Ekranı | Giriş Gerekli | Hedef kullanıcı seçimi, ilişki türü |
| SCR-092 | Tanıma Cevabı Ekranı | Giriş Gerekli | "Evet / Hayır" sorusu |
| SCR-093 | Referans Yazma Ekranı | Giriş Gerekli | Değerlendirme alanları |
| SCR-094 | Referans Detay Ekranı | Giriş Gerekli | Onaylı referansın tüm detayları |
| SCR-095 | Referans Cevabı Yazma Ekranı | Giriş Gerekli | Kendi hakkındaki referansa cevap |
| SCR-096 | Referans İtiraz Ekranı | Giriş Gerekli | İtiraz gerekçesi girişi |
| SCR-097 | Referans İtiraz Detayı | Giriş Gerekli | İtiraz durumu |

---

## 11. Kira Sözleşmesi (feature-contracts)

| ID | Ekran Adı | Erişim | Notlar |
|---|---|---|---|
| SCR-100 | Sözleşme Listesi | Giriş Gerekli | Aktif + Taslak + Geçmiş |
| SCR-101 | Yeni Sözleşme — Şablon Seçimi | Giriş Gerekli | |
| SCR-102 | Sözleşme Formu — Temel Bilgiler | Giriş Gerekli | Adres, taraflar, kira |
| SCR-103 | Sözleşme Formu — Detaylar | Giriş Gerekli | Depozito, tarihler, aidat |
| SCR-104 | Sözleşme Formu — Demirbaş Listesi | Giriş Gerekli | Eşya listesi, teslim durumu |
| SCR-105 | Sözleşme Formu — Özel Şartlar | Giriş Gerekli | Kefil, ek açıklamalar |
| SCR-106 | Sözleşme Önizleme Ekranı | Giriş Gerekli | Paylaşmadan önce tam görünüm |
| SCR-107 | Sözleşme Detay Ekranı | Giriş Gerekli | Kilitli/onaylı sözleşme görünümü |
| SCR-108 | Düzenleme Talebi Ekranı | Giriş Gerekli | İkinci taraf düzenleme notu girer |
| SCR-109 | PDF İndirme / Paylaşma Ekranı | Giriş Gerekli | İndirme veya paylaşma seçenekleri |

---

## 12. Bildirimler (feature-notifications)

| ID | Ekran Adı | Erişim | Notlar |
|---|---|---|---|
| SCR-110 | Bildirim Merkezi | Giriş Gerekli | Tüm bildirimler listesi |
| SCR-111 | Bildirim Detayı | Giriş Gerekli | Bildirime tıklanınca ilgili içeriğe yönlendirme |

---

## 13. Profil (feature-profile)

| ID | Ekran Adı | Erişim | Notlar |
|---|---|---|---|
| SCR-120 | Kendi Profil Ekranı | Giriş Gerekli | Gönderiler, referanslar, sözleşmeler, ayarlar |
| SCR-121 | Başka Kullanıcı Profil Ekranı | Giriş Gerekli | Onaylı gönderiler, referanslar |
| SCR-122 | Profil Düzenleme Ekranı | Giriş Gerekli | Ad, biyografi, şehir, fotoğraf |
| SCR-123 | Profil Fotoğrafı Seçme Ekranı | Giriş Gerekli | Galeri/kamera |
| SCR-124 | Kaydedilen Gönderiler | Giriş Gerekli | |
| SCR-125 | Engellenen Kullanıcılar | Giriş Gerekli | |

---

## 14. Ayarlar (feature-settings)

| ID | Ekran Adı | Erişim | Notlar |
|---|---|---|---|
| SCR-130 | Ayarlar Ana Ekranı | Giriş Gerekli | Tüm ayar kategorileri |
| SCR-131 | Hesap Ayarları | Giriş Gerekli | E-posta, şifre, bağlı hesaplar |
| SCR-132 | Şifre Değiştirme Ekranı | Giriş Gerekli | |
| SCR-133 | Gizlilik Ayarları | Giriş Gerekli | Profil görünürlüğü, referans görünürlüğü |
| SCR-134 | Bildirim Ayarları | Giriş Gerekli | Bildirim türleri açık/kapalı |
| SCR-135 | Tema Seçimi | Giriş Gerekli | Açık / Koyu / Sistem |
| SCR-136 | Hakkında Ekranı | Herkese Açık | Uygulama sürümü, lisanslar |
| SCR-137 | Kullanım Koşulları | Herkese Açık | WebView veya statik metin |
| SCR-138 | Gizlilik Politikası | Herkese Açık | |
| SCR-139 | KVKK Aydınlatma Metni | Herkese Açık | |
| SCR-140 | Açık Rıza Ayarları | Giriş Gerekli | |
| SCR-141 | Topluluk Kuralları | Herkese Açık | |
| SCR-142 | Referans Sistemi Kuralları | Herkese Açık | |
| SCR-143 | Sözleşme Kullanım Koşulları | Herkese Açık | |
| SCR-144 | Çerez ve Üçüncü Taraf Hizmetler | Herkese Açık | |
| SCR-145 | Yardım ve Destek | Herkese Açık | |
| SCR-146 | SSS | Herkese Açık | |
| SCR-147 | Açık Kaynak Lisansları | Herkese Açık | |
| SCR-148 | Veri İndirme Talebi | Giriş Gerekli | |
| SCR-149 | Hesap Silme Ekranı | Giriş Gerekli | Onay diyaloğu + kimlik doğrulama |

---

## 15. Satın Alma / Reklam (feature-billing, feature-ads)

| ID | Ekran Adı | Erişim | Notlar |
|---|---|---|---|
| SCR-150 | Reklam Kaldırma Satın Alma Ekranı | Giriş Gerekli | Ürün açıklaması, Google Play Billing |
| SCR-151 | Satın Alma Başarı Ekranı | Giriş Gerekli | |
| SCR-152 | Satın Alma Geri Yükleme Ekranı | Giriş Gerekli | |

---

## 16. Hata ve Boş Durum Ekranları

| ID | Ekran Adı | Kullanım Yeri |
|---|---|---|
| SCR-160 | Genel Hata Ekranı | Ağ hatası, Firebase hatası |
| SCR-161 | Boş Durum Bileşeni | Akış boşsa, bildirim yoksa vb. |
| SCR-162 | İnternet Bağlantısı Yok Bileşeni | Çevrimdışı durumda |
| SCR-163 | Yükleniyor Bileşeni | Skeleton / Shimmer |
| SCR-164 | 404 / Bulunamadı Ekranı | Silinen içeriğe deep link ile erişim |

---

## Ekran Sayısı Özeti

| Kategori | Ekran Sayısı |
|---|---|
| Kimlik Doğrulama | 8 |
| Onboarding | 4 |
| Ana Navigasyon | 5 |
| Ana Akış | 2 |
| Keşfet | 3 |
| Gönderi | 6 |
| Yorumlar | 3 |
| Etkileşimler | 1 |
| Şikâyet | 3 |
| Referans | 8 |
| Kira Sözleşmesi | 10 |
| Bildirimler | 2 |
| Profil | 6 |
| Ayarlar | 20 |
| Satın Alma / Reklam | 3 |
| Hata / Boş Durum | 5 |
| **TOPLAM** | **89** |

---

*Sonraki doküman: 09-navigation-map.md*