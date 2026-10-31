# Mülkora — Fonksiyonel Gereksinimler

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## FR-01 Kimlik Doğrulama ve Oturum Yönetimi

### FR-01.1 Kayıt

- FR-01.1.1: Sistem, kullanıcıya e-posta ve şifre ile kayıt olma imkânı sunmalıdır.
- FR-01.1.2: Sistem, Google hesabı ile kayıt ve giriş yapılmasına izin vermelidir.
- FR-01.1.3: Kayıt sırasında görünen ad, benzersiz kullanıcı adı, e-posta, kullanıcı rolü, şehir bilgisi alınmalıdır.
- FR-01.1.4: Kayıt sırasında isteğe bağlı profil fotoğrafı ve kısa biyografi alınabilir.
- FR-01.1.5: Kayıt sırasında kullanım koşulları, gizlilik politikası, KVKK aydınlatma metni, topluluk kuralları ve 18 yaş onayı alınmalıdır.
- FR-01.1.6: Kullanıcı adı benzersizliği Cloud Functions üzerinden kontrol edilmelidir.
- FR-01.1.7: Kullanıcı kendi hesabına yönetici veya moderatör rolü atayamaz.
- FR-01.1.8: Kayıt sonrasında e-posta doğrulama bağlantısı gönderilmelidir.

### FR-01.2 Giriş

- FR-01.2.1: Sistem, e-posta ve şifre ile girişi desteklemelidir.
- FR-01.2.2: Sistem, Google ile girişi desteklemelidir.
- FR-01.2.3: Engellenen kullanıcı uygulamaya giriş yapamamalıdır.
- FR-01.2.4: E-postası doğrulanmamış kullanıcı içerik oluşturamaz; yalnızca okuma yapabilir. (Varsayım: Google ile girişte doğrulama otomatik kabul edilir.)

### FR-01.3 Şifre ve Hesap Yönetimi

- FR-01.3.1: Kullanıcı şifresini sıfırlayabilmelidir (e-posta ile sıfırlama bağlantısı).
- FR-01.3.2: Kullanıcı oturumunu kapatabilmelidir.
- FR-01.3.3: Kullanıcı hesabını kalıcı olarak silebilmelidir.
- FR-01.3.4: Hesap silindiğinde ilgili veriler KVKK gerekliliklerine göre silinir veya anonimleştirilir.

---

## FR-02 Profil Yönetimi

- FR-02.1: Kullanıcı profilinde profil fotoğrafı, kullanıcı adı, görünen ad, rol, şehir, biyografi ve üyelik tarihi yer alır.
- FR-02.2: Kullanıcı kendi profilini düzenleyebilir (görünen ad, biyografi, şehir, profil fotoğrafı).
- FR-02.3: Profil sayfasında kullanıcının onaylanmış gönderileri, yorumları ve aldığı referanslar gösterilir.
- FR-02.4: Başka kullanıcının profilinde şikâyet ve engelleme seçeneği bulunur.
- FR-02.5: Engellenen kullanıcılar listesi yalnızca engel koyan kullanıcı tarafından görüntülenebilir.
- FR-02.6: Kullanıcı başka bir kullanıcıyı engelleyebilir; engelleme karşılıklı etkileşimi kısıtlar.

---

## FR-03 Ana Akış (Feed)

- FR-03.1: Ana akışta yalnızca yönetici veya moderatör tarafından onaylanmış gönderiler gösterilir.
- FR-03.2: Akış Paging 3 ile sayfalanır; sonsuz kaydırma desteklenir.
- FR-03.3: Her gönderi kartında profil fotoğrafı, kullanıcı adı, rol rozeti, tarih, kategori, tür, başlık, metin, isteğe bağlı görsel, beğeni, eksi oy, yorum sayısı, ortalama puan, kaydetme, şikâyet ve paylaşma öğeleri bulunur.
- FR-03.4: Akış şu filtrelerle filtrelenebilir: en yeni, en çok beğenilen, en çok yorumlanan, en yüksek puanlı, kiracı gönderileri, ev sahibi gönderileri, soru, deneyim, şehir bazlı.
- FR-03.5: Anonim paylaşılan gönderilerde kullanıcı adı ve profil fotoğrafı gizlenir.

---

## FR-04 Gönderi Oluşturma

- FR-04.1: Gönderi türleri: Soru, Deneyim, Sorun, Tavsiye, Bilgilendirme, Tartışma.
- FR-04.2: Gönderi kategorileri: Kira artışı, Depozito, Aidat, Ev bakımı, Tadilat, Gürültü, Komşuluk, Tahliye, Kira sözleşmesi, Ev sahibi ve kiracı iletişimi, Faturalar, Taşınma, Hasar ve bakım, Genel.
- FR-04.3: Kullanıcı gönderiye isteğe bağlı tek bir görsel ekleyebilir.
- FR-04.4: Kullanıcı anonim paylaşım tercihini seçebilir.
- FR-04.5: Kullanıcı yorumlara izin verip vermeyeceğini seçebilir.
- FR-04.6: Gönderi oluşturulduktan sonra otomatik içerik filtresinden geçer.
- FR-04.7: Filtreden geçen gönderi moderasyon kuyruğuna alınır.
- FR-04.8: Kullanıcı, kendi onay bekleyen ve reddedilen gönderilerini profilinden görebilir.
- FR-04.9: Kullanıcı taslak oluşturabilir (yayımlamadan kaydedebilir).

---

## FR-05 Gönderi Detayı ve Etkileşimler

- FR-05.1: Gönderi detay sayfasında tüm gönderi bilgileri, yorumlar ve etkileşim seçenekleri gösterilir.
- FR-05.2: Kullanıcı gönderiye beğeni verebilir.
- FR-05.3: Kullanıcı gönderiye eksi oy verebilir.
- FR-05.4: Beğeni ve eksi oy aynı anda verilemez; biri seçilirse diğeri otomatik kaldırılır.
- FR-05.5: Kullanıcı beğenisini veya eksi oyunu kaldırabilir.
- FR-05.6: Kullanıcı gönderiye 1–5 yıldız puan verebilir; her kullanıcı bir gönderiye yalnızca bir kez puan verebilir.
- FR-05.7: Kullanıcı gönderiyi kaydedebilir.
- FR-05.8: Kullanıcı gönderiyi şikâyet edebilir.
- FR-05.9: Kullanıcı gönderi bağlantısını kopyalayabilir veya paylaşabilir.

---

## FR-06 Yorum ve Cevap Sistemi

- FR-06.1: Kullanıcı onaylanmış bir gönderiye yorum yazabilir (gönderi sahibi yorumlara izin verdiyse).
- FR-06.2: Kullanıcı bir yoruma cevap yazabilir.
- FR-06.3: Cevaplar açılıp kapatılabilir.
- FR-06.4: Kullanıcı bir yorumu beğenebilir.
- FR-06.5: Kullanıcı kendi yorumunu düzenleyebilir.
- FR-06.6: Kullanıcı kendi yorumunu silebilir.
- FR-06.7: Kullanıcı bir yorumu şikâyet edebilir.
- FR-06.8: Yorum sahibinin rolü (kiracı/ev sahibi) yorum üzerinde gösterilir.
- FR-06.9: Yorumlar içerik filtresinden geçirilir; yüksek riskli yorumlar moderasyon kuyruğuna alınır.

---

## FR-07 Moderasyon Akışı

- FR-07.1: Gönderi durumları: taslak, inceleme bekliyor, otomatik filtreye takıldı, yönetici incelemesinde, düzenleme bekliyor, onaylandı, reddedildi, yayından kaldırıldı, silindi.
- FR-07.2: Moderatör onay bekleyen gönderileri görebilir, onaylayabilir, reddedebilir veya düzenleme isteyebilir.
- FR-07.3: Moderatör şikâyetleri değerlendirebilir.
- FR-07.4: Moderatör referansları ve referans cevaplarını inceleyebilir.
- FR-07.5: Moderatör kullanıcılara uyarı verebilir.
- FR-07.6: Moderatör içerikleri geçici olarak gizleyebilir.
- FR-07.7: Gönderi onaylandığında, reddedildiğinde veya düzenleme istendiğinde kullanıcıya bildirim gönderilir.

---

## FR-08 Şikâyet Sistemi

- FR-08.1: Şikâyet edilebilecek içerikler: gönderi, yorum, kullanıcı profili, referans, referans cevabı.
- FR-08.2: Şikâyet nedenleri: hakaret veya küfür, taciz, tehdit, nefret söylemi, spam, yanlış bilgi, kişisel bilgi paylaşımı, iftira veya hedef gösterme, uygunsuz görsel, reklam, konu dışı içerik, sahte referans, diğer.
- FR-08.3: Aynı kullanıcı aynı içeriği yalnızca bir kez şikâyet edebilir.
- FR-08.4: Şikâyet durumları: yeni, inceleniyor, ek bilgi bekleniyor, işlem yapıldı, reddedildi, kapatıldı.
- FR-08.5: Şikâyet sonucu kullanıcıya bildirim olarak iletilir.

---

## FR-09 Küfür ve Zararlı İçerik Filtresi

- FR-09.1: Filtre şu yöntemleri algılar: tam kelime eşleşmesi, büyük/küçük harf değişikliği, Türkçe karakter değişimi, harf yerine rakam, boşluk/nokta ile bölme, harf tekrarları, sembollerle filtre aşma, benzer yazılışlar, telefon numarası, T.C. kimlik numarası, açık adres, IBAN, tehdit ifadeleri, ırkçı söylemler.
- FR-09.2: Risk seviyeleri: Düşük (uyarı göster), Orta (moderasyon kuyruğuna gönder), Yüksek (gönderimi engelle), Kritik (engelle ve yöneticiyi bilgilendir).
- FR-09.3: Yasaklı kelime listesi yönetim panelinden güncellenebilir.
- FR-09.4: Filtre gönderi metni, başlık ve yorumlara uygulanır.

---

## FR-10 Referans Sistemi

- FR-10.1: Kullanıcı başka bir kullanıcı hakkında referans oluşturma talebi başlatabilir.
- FR-10.2: Sistem hedef kullanıcıya "Bu kişiyi tanıyor musunuz?" sorusunu gönderir.
- FR-10.3: Hedef kullanıcı "Evet" veya "Hayır" cevabı verir.
- FR-10.4: "Hayır" cevabında süreç durur; referans yayımlanmaz.
- FR-10.5: "Evet" cevabında referans hazırlama ekranı açılır.
- FR-10.6: İlişki türleri: önceki kiracım, önceki ev sahibim, mevcut kiracım, mevcut ev sahibim.
- FR-10.7: Kiracı değerlendirme alanları: kira ödeme düzeni, evi kullanım özeni, iletişim, komşuluk kurallarına uyum, teslim süreci, genel değerlendirme.
- FR-10.8: Ev sahibi değerlendirme alanları: iletişim, bakım ve onarım yaklaşımı, depozito süreci, sözleşmeye uyum, mahremiyete saygı, genel değerlendirme.
- FR-10.9: Her alan için 1–5 puan, olumlu/tarafsız/olumsuz seçimi ve açıklama yazılabilir.
- FR-10.10: Referans yönetici onayına gönderilir; onaylananlar hedef kullanıcının profilinde yayımlanır.
- FR-10.11: Hedef kullanıcı referansa cevap yazabilir; cevap da yönetici onayından geçer.
- FR-10.12: Hedef kullanıcı referansa itiraz edebilir; itiraz yönetim paneline düşer.
- FR-10.13: Referans durumları: davet gönderildi, tanıma onayı bekleniyor, tanınmadı, referans hazırlanıyor, yönetici onayı bekliyor, onaylandı, reddedildi, itiraz edildi, incelemeye alındı, gizlendi, silindi.

---

## FR-11 Kira Sözleşmesi

- FR-11.1: Kullanıcı yeni kira sözleşmesi taslağı oluşturabilir.
- FR-11.2: Sözleşme şablonu seçilebilir.
- FR-11.3: Form alanları: sözleşme tarihi, taşınmaz adresi, taşınmaz türü, ev sahibi bilgileri, kiracı bilgileri, aylık kira, ödeme günü, ödeme yöntemi, depozito, başlangıç/bitiş tarihi, aidat bilgileri, fatura sorumlulukları, demirbaş listesi, evdeki eşyalar, teslim durumu, özel şartlar, kefil bilgileri, ek açıklamalar.
- FR-11.4: İkinci taraf uygulama üzerinden davet edilebilir.
- FR-11.5: İkinci taraf sözleşmeyi inceleyebilir ve düzenleme talebinde bulunabilir.
- FR-11.6: Her iki tarafın onayından sonra sözleşme sürümü kilitlenir.
- FR-11.7: Sistem PDF oluşturur; kullanıcı indirebilir veya paylaşabilir.
- FR-11.8: PDF içeriği: başlık, taraf bilgileri, maddeler, demirbaş listesi, oluşturulma tarihi, sürüm, benzersiz belge numarası, doğrulama kodu, bütünlük hash değeri, sayfa numaraları.
- FR-11.9: Sözleşme yalnızca taraflar ve yetkili yöneticiler tarafından okunabilir.

---

## FR-12 Bildirim Sistemi

- FR-12.1: Bildirimler Firebase Cloud Messaging ile push olarak gönderilir.
- FR-12.2: Bildirimler uygulama içi bildirim merkezinde saklanır.
- FR-12.3: Bildirim türleri:
  - Gönderi onaylandı / reddedildi / düzenleme istendi
  - Gönderiye yorum yapıldı
  - Yoruma cevap verildi
  - Gönderi beğenildi
  - Referans talebi geldi
  - "Bu kişiyi tanıyor musunuz?" sorusu geldi
  - Referans onaylandı / reddedildi
  - Referansa cevap geldi / itiraz edildi
  - Sözleşme daveti / güncellemesi / onay bekleniyor
  - PDF hazırlandı
  - Şikâyet sonucu açıklandı
  - Hesaba uyarı verildi
  - Yönetici duyurusu
- FR-12.4: Kullanıcı bildirim tercihlerini yönetebilir.

---

## FR-13 Reklam Sistemi

- FR-13.1: AdMob native, banner ve sınırlı interstitial reklam türleri kullanılır.
- FR-13.2: Ana akışın ilk 4 gönderisinde reklam gösterilmez.
- FR-13.3: Sonrasında yaklaşık her 8–10 gönderide bir native reklam gösterilir.
- FR-13.4: Gönderi oluşturma, kayıt/giriş, şikâyet, referans yazma, sözleşme düzenleme ve PDF hazırlama ekranlarında reklam gösterilmez.
- FR-13.5: FrequencyCapManager uygulanır: 10 dakikada en fazla 1 interstitial, günlük sınır.
- FR-13.6: Uygulama açıldıktan hemen sonra reklam gösterilmez.
- FR-13.7: Debug sürümünde test reklam kimlikleri kullanılır.

---

## FR-14 Satın Alma (Reklam Kaldırma)

- FR-14.1: Ayarlarda "Reklamları Kaldır" seçeneği bulunur.
- FR-14.2: Ürün: `remove_ads_lifetime` — tek seferlik, kalıcı, tüketilmeyen.
- FR-14.3: Satın alma sonrası tüm reklam türleri gizlenir.
- FR-14.4: "Satın Almaları Geri Yükle" seçeneği bulunur.
- FR-14.5: Satın alma tokenı backend (Cloud Functions) üzerinden Google Play API ile doğrulanır.
- FR-14.6: Satın alma durumu Firestore'da saklanır; uygulama yeniden kurulduğunda geri yüklenebilir.

---

## FR-15 Ayarlar ve Hukuki Ekranlar

- FR-15.1: Hesap ayarları: profil düzenle, e-posta ayarları, şifre değiştir, bağlı Google hesabı, oturumu kapat, hesabı sil.
- FR-15.2: Gizlilik ayarları: profil görünürlüğü, referans görünürlüğü, bildirim tercihleri, engellenen kullanıcılar, veri indirme talebi, hesap ve veri silme talebi.
- FR-15.3: Reklam ve satın alma: reklamları kaldır, geri yükle, satın alma durumu.
- FR-15.4: Uygulama ayarları: açık/koyu/sistem teması, dil için altyapı, bildirim ayarları.
- FR-15.5: Bilgilendirme: uygulama hakkında, kullanım koşulları, gizlilik politikası, KVKK aydınlatma, açık rıza, topluluk kuralları, referans kuralları, sözleşme kullanım koşulları, çerez ve üçüncü taraf, yardım, SSS, açık kaynak lisansları.
- FR-15.6: Uygulama sürümü `versionName` üzerinden dinamik olarak gösterilir.

---

## FR-16 Web Yönetim Paneli

- FR-16.1: Yönetim paneli ayrı bir web uygulamasıdır (Next.js/React + TypeScript + Firebase Hosting).
- FR-16.2: Bölümler: genel gösterge, onay bekleyen gönderiler, yayımlanan gönderiler, reddedilen gönderiler, yorum yönetimi, şikâyetler, referans talepleri, onay bekleyen referanslar, referans cevapları, referans itirazları, kullanıcılar, engellenen kullanıcılar, moderatör yönetimi, kategoriler, yasaklı kelimeler, sözleşme şablonları, reklam ayarları, bildirim gönderimi, sistem ayarları, denetim kayıtları, istatistikler.
- FR-16.3: Yönetici ve moderatör yetkileri Firebase custom claims ile doğrulanır.
- FR-16.4: Her yönetici işlemi denetim kaydına (auditLogs) yazılır.

---

*Sonraki doküman: 03-non-functional-requirements.md*
