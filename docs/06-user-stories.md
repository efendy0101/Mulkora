# Mülkora — Kullanıcı Hikâyeleri

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## Biçim

> **US-[numara]:** Bir [rol] olarak, [ihtiyaç], çünkü [değer/gerekçe].  
> **Kabul Kriterleri:** ...  
> **Öncelik:** Yüksek / Orta / Düşük  
> **Aşama:** MVP / v2 / v3

---

## 1. Kimlik Doğrulama ve Hesap

### US-001 — E-posta ile kayıt

**Bir yeni kullanıcı olarak,** e-posta adresim ve şifremle platforma kayıt olmak istiyorum, çünkü uygulamayı kullanmaya başlamak için hesabıma ihtiyacım var.

**Kabul Kriterleri:**
- Kayıt formu görünen ad, kullanıcı adı, e-posta, şifre, rol seçimi ve şehir alanlarını içerir.
- Kullanıcı adı benzersiz olmalı; mevcut bir kullanıcı adı girilirse anlık hata mesajı gösterilir.
- Şifre minimum 8 karakter olmalı ve güç göstergesi sunulur.
- Kullanım koşulları, gizlilik politikası, KVKK, topluluk kuralları ve 18 yaş onay kutucukları işaretlenmedikçe kayıt tamamlanamaz.
- Kayıt sonrası e-posta doğrulama bağlantısı gönderilir.
- Kullanıcı başarılı kayıt sonrası ana akışa yönlendirilir.

**Öncelik:** Yüksek | **Aşama:** MVP

---

### US-002 — Google ile giriş

**Bir kullanıcı olarak,** Google hesabımla hızla giriş yapmak istiyorum, çünkü ayrı bir şifre oluşturmak istemiyorum.

**Kabul Kriterleri:**
- "Google ile Giriş Yap" butonu giriş ekranında görünür.
- Birinci kez giriş yapan Google kullanıcısı kullanıcı adı ve rol seçimi ekranına yönlendirilir.
- Daha önce kayıtlı Google kullanıcısı doğrudan ana akışa geçer.
- Google ile giriş yapıldıktan sonra e-posta doğrulaması aranmaz.

**Öncelik:** Yüksek | **Aşama:** MVP

---

### US-003 — Şifre sıfırlama

**Şifresini unutan bir kullanıcı olarak,** e-posta adresime sıfırlama bağlantısı alarak yeni şifre belirlemek istiyorum.

**Kabul Kriterleri:**
- Giriş ekranında "Şifremi Unuttum" bağlantısı bulunur.
- Geçerli bir e-posta girilirse Firebase şifre sıfırlama e-postası gönderilir.
- Sistemde kayıtlı olmayan e-posta adresi girildiğinde genel bir mesaj gösterilir (güvenlik gereği kullanıcı adı enumeration yapılmaz).

**Öncelik:** Yüksek | **Aşama:** MVP

---

### US-004 — Hesap silme

**Bir kullanıcı olarak,** hesabımı kalıcı olarak silmek istiyorum, çünkü platformdan ayrılmak ve verilerimin silinmesini talep etmek istiyorum.

**Kabul Kriterleri:**
- Ayarlar ekranında "Hesabı Sil" seçeneği bulunur.
- Silme işlemi öncesinde onay diyaloğu gösterilir.
- Hesap silme Cloud Functions üzerinden gerçekleştirilir.
- Kullanıcı verileri KVKK uyarınca silinir veya anonimleştirilir.
- Kullanıcı silme işlemi sonrası uygulamadan çıkış yaptırılır.

**Öncelik:** Yüksek | **Aşama:** MVP

---

## 2. Profil

### US-005 — Profil oluşturma

**Yeni kayıt olan bir kullanıcı olarak,** profilimi oluşturmak istiyorum, çünkü kimliğimi toplulukta temsil etmek istiyorum.

**Kabul Kriterleri:**
- Profil fotoğrafı yüklenebilir (isteğe bağlı).
- Görünen ad, kullanıcı adı, şehir ve kısa biyografi girilebilir.
- Profil kaydedildiğinde Firestore'da güncellenir.

**Öncelik:** Yüksek | **Aşama:** MVP

---

### US-006 — Başka kullanıcının profilini görüntüleme

**Bir kullanıcı olarak,** başka bir kullanıcının profiline girerek gönderilerini, referanslarını ve rolünü görmek istiyorum.

**Kabul Kriterleri:**
- Profil sayfasında ad, kullanıcı adı, rol rozeti, şehir, biyografi, üyelik tarihi görünür.
- Onaylanmış gönderileri ve aldığı referanslar listelenir.
- Şikâyet ve engelleme seçenekleri mevcut kullanıcı için görünür.

**Öncelik:** Yüksek | **Aşama:** MVP

---

## 3. Ana Akış

### US-007 — Ana akışı görüntüleme

**Bir kullanıcı olarak,** ana akışta onaylanmış gönderileri görmek istiyorum, çünkü topluluğun deneyimlerinden haberdar olmak istiyorum.

**Kabul Kriterleri:**
- Yalnızca onaylı gönderiler gösterilir.
- Her gönderi kartında profil fotoğrafı, kullanıcı adı, rol rozeti, tarih, kategori, tür, başlık, metin özeti, görsel (varsa), beğeni, eksi oy, yorum sayısı, ortalama puan, kaydet ve şikâyet seçenekleri bulunur.
- Sonsuz kaydırma Paging 3 ile çalışır.
- Akış filtre seçenekleri (en yeni, en çok beğenilen vb.) kullanılabilir.

**Öncelik:** Yüksek | **Aşama:** MVP

---

### US-008 — Akışı filtreleme

**Bir kullanıcı olarak,** akışı "kiracı gönderileri", "en çok yorumlanan" veya "şehir bazlı" gibi filtrelerle düzenlemek istiyorum, çünkü ilgimi çeken içeriklere daha hızlı ulaşmak istiyorum.

**Kabul Kriterleri:**
- Filtre seçenekleri ekranın üst kısmında erişilebilir.
- Seçilen filtre akışı gerçek zamanlı olarak günceller.
- Filtre durumu sayfa geçişlerinden sonra korunur.

**Öncelik:** Yüksek | **Aşama:** MVP

---

## 4. Gönderi Oluşturma

### US-009 — Gönderi paylaşma

**Bir kiracı olarak,** ev sahibiyle yaşadığım sorunla ilgili bir gönderi paylaşmak istiyorum, çünkü benzer durumda olan kişilere yardımcı olmak ve tavsiye almak istiyorum.

**Kabul Kriterleri:**
- Gönderi oluşturma ekranında tür, kategori, başlık, açıklama, şehir, görsel (isteğe bağlı), anonim paylaşım ve yorum izni seçenekleri bulunur.
- Gönderi gönderildiğinde içerik filtresinden geçirilir.
- Düşük risk: uyarı mesajı gösterilir, kullanıcı devam edebilir.
- Yüksek risk: gönderi engellenir.
- Filtreden geçen gönderi "inceleme bekliyor" durumuna alınır.
- Kullanıcıya "Gönderiniz incelemeye alındı" bildirimi gösterilir.

**Öncelik:** Yüksek | **Aşama:** MVP

---

### US-010 — Anonim gönderi paylaşma

**Bir kullanıcı olarak,** kimliğimi gizleyerek anonim gönderi paylaşmak istiyorum, çünkü hassas konuları kimliğim açık olmadan paylaşmak istiyorum.

**Kabul Kriterleri:**
- Gönderi oluşturma formunda "Anonim paylaş" seçeneği bulunur.
- Anonim gönderilerde akışta kullanıcı adı ve fotoğraf yerine "Anonim Kullanıcı" gösterilir.
- Sunucu tarafında gönderi sahibi bilinir; yönetici moderasyon için kim olduğunu görebilir.

**Öncelik:** Orta | **Aşama:** MVP

---

## 5. Etkileşimler

### US-011 — Gönderi beğenme

**Bir kullanıcı olarak,** faydalı bulduğum gönderileri beğenmek istiyorum, çünkü kaliteli içeriği desteklemek istiyorum.

**Kabul Kriterleri:**
- Beğeni butonu gönderi kartında ve detay sayfasında bulunur.
- Beğeni işlemi Cloud Functions üzerinden gerçekleşir.
- Beğeni sayısı anlık güncellenir.
- Zaten beğenilmiş bir gönderiye tıklanırsa beğeni kaldırılır.
- Beğeni verilen gönderiye eksi oy verilirse beğeni otomatik kaldırılır.

**Öncelik:** Yüksek | **Aşama:** MVP

---

### US-012 — Gönderi puanlama

**Bir kullanıcı olarak,** bir gönderiye 1–5 yıldız puan vermek istiyorum, çünkü içeriğin kalitesini değerlendirmek istiyorum.

**Kabul Kriterleri:**
- Puanlama yıldız bileşeni gönderi detay sayfasında bulunur.
- Bir kullanıcı aynı gönderiye yalnızca bir kez puan verebilir.
- İkinci kez puan vermek yerine önceki puanı değiştirir.
- Puanlama Cloud Functions üzerinden gerçekleşir.
- Ortalama puan gönderi kartında ve detayda gösterilir.

**Öncelik:** Yüksek | **Aşama:** MVP

---

## 6. Yorum ve Cevap

### US-013 — Yorum yazma

**Bir kullanıcı olarak,** bir gönderiye yorum yazarak deneyimimi paylaşmak veya soru soran kişiye cevap vermek istiyorum.

**Kabul Kriterleri:**
- Yorum alanı gönderi detay sayfasının altında yer alır.
- Yorum gönderildiğinde içerik filtresinden geçirilir.
- Yüksek riskli yorum moderasyon kuyruğuna alınır; kullanıcıya bilgi verilir.
- Yorum sayısı gönderi kartında güncellenir.

**Öncelik:** Yüksek | **Aşama:** MVP

---

### US-014 — Kendi yorumunu düzenleme

**Bir kullanıcı olarak,** yazdığım yorumda hata yaptığımda düzenlemek istiyorum.

**Kabul Kriterleri:**
- Kendi yorumunda "Düzenle" seçeneği görünür.
- Düzenlenen yorum yeniden içerik filtresinden geçirilir.
- Yorumda düzenlendiğine dair işaret (düzenlendi rozeti veya tarihi) gösterilir.

**Öncelik:** Orta | **Aşama:** MVP

---

## 7. Şikâyet Sistemi

### US-015 — İçerik şikâyet etme

**Bir kullanıcı olarak,** uygunsuz bulduğum bir gönderiyi, yorumu veya kullanıcıyı şikâyet etmek istiyorum, çünkü platformun güvenli kalmasına katkıda bulunmak istiyorum.

**Kabul Kriterleri:**
- Şikâyet seçeneği gönderi, yorum ve profil sayfalarında bulunur.
- Şikâyet nedenlerinin listesi açılır.
- Aynı içerik aynı kullanıcı tarafından yalnızca bir kez şikâyet edilebilir; tekrar denenirse uyarı mesajı gösterilir.
- Şikâyet başarılı olduğunda kullanıcıya onay mesajı gösterilir.

**Öncelik:** Yüksek | **Aşama:** MVP

---

## 8. Referans Sistemi

### US-016 — Ev sahibi hakkında referans başlatma

**Bir kiracı olarak,** daha önce kiraladığım dairenin ev sahibi hakkında referans oluşturmak istiyorum, çünkü diğer kiracıların bilinçli karar vermesine yardımcı olmak istiyorum.

**Kabul Kriterleri:**
- Kullanıcı profilinden veya referans bölümünden referans başlatılabilir.
- Hedef kullanıcıya "Bu kişiyi tanıyor musunuz?" bildirimi gönderilir.
- "Hayır" cevabı gelirse süreç durur ve kullanıcıya bilgi verilir.
- "Evet" cevabı gelirse referans hazırlama ekranı açılır.

**Öncelik:** Yüksek | **Aşama:** MVP

---

### US-017 — Referans yazma

**Bir kiracı olarak,** "Evet" cevabı aldıktan sonra ev sahibiyle ilgili değerlendirme alanlarını doldurarak referans yazmak istiyorum.

**Kabul Kriterleri:**
- İlişki türü seçilir (önceki ev sahibim, mevcut ev sahibim).
- Her değerlendirme alanı için 1–5 puan, olumlu/tarafsız/olumsuz seçimi ve açıklama yazılabilir.
- Referans gönderildiğinde yönetici moderasyon kuyruğuna alınır.
- Onaylanan referans hedef kullanıcının profilinde görünür.

**Öncelik:** Yüksek | **Aşama:** MVP

---

### US-018 — Referansa itiraz etme

**Hakkımda yanlış bilgi içeren bir referans alan kullanıcı olarak,** bu referansa itiraz etmek istiyorum, çünkü gerçeği yansıtmayan içeriğin profilimde kalmasını istemiyorum.

**Kabul Kriterleri:**
- Kendi profilindeki onaylı referansın yanında "İtiraz Et" butonu bulunur.
- İtiraz gerekçesi yazılabilir.
- İtiraz yönetim paneline düşer.
- Kullanıcıya itirazın alındığı bildirimi gönderilir.

**Öncelik:** Yüksek | **Aşama:** MVP

---

## 9. Kira Sözleşmesi

### US-019 — Kira sözleşmesi oluşturma

**Bir ev sahibi olarak,** kiracımla birlikte uygulama üzerinden standart kira sözleşmesi hazırlamak istiyorum, çünkü geçerli ve anlaşmalı bir belge oluşturmak istiyorum.

**Kabul Kriterleri:**
- Sözleşme şablonu seçilebilir.
- Tüm form alanları doldurulabilir.
- İkinci taraf (kiracı) uygulama üzerinden davet edilebilir.
- İkinci taraf sözleşmeyi inceleyebilir ve düzenleme talep edebilir.
- Her iki tarafın onayından sonra sözleşme kilitlenir.

**Öncelik:** Yüksek | **Aşama:** MVP

---

### US-020 — PDF sözleşme indirme

**Bir kiracı olarak,** onaylanan kira sözleşmesini PDF olarak indirmek istiyorum, çünkü sözleşmenin bir kopyasına sahip olmak istiyorum.

**Kabul Kriterleri:**
- PDF indirme butonu sözleşme detay sayfasında bulunur.
- PDF'de benzersiz belge numarası, doğrulama kodu ve hash değeri yer alır.
- PDF yalnızca sözleşmenin tarafları tarafından indirilebilir.
- PDF üretimi backend Cloud Functions üzerinde gerçekleşir.

**Öncelik:** Yüksek | **Aşama:** MVP

---

## 10. Bildirimler

### US-021 — Gönderi durumu bildirimi alma

**Bir kullanıcı olarak,** gönderim onaylandığında veya reddedildiğinde bildirim almak istiyorum, çünkü moderasyon durumunu takip etmek istiyorum.

**Kabul Kriterleri:**
- Gönderi onaylandığında push bildirim ve uygulama içi bildirim gönderilir.
- Gönderi reddedildiğinde red gerekçesiyle birlikte bildirim gönderilir.
- Düzenleme istendiğinde kullanıcı bilgilendirilir.
- Bildirimler bildirim merkezi listesinde saklanır.

**Öncelik:** Yüksek | **Aşama:** MVP

---

### US-022 — Bildirim tercihlerini yönetme

**Bir kullanıcı olarak,** hangi bildirim türlerini almak istediğimi seçmek istiyorum, çünkü ilgimi çekmeyen bildirimlerden rahatsız olmak istemiyorum.

**Kabul Kriterleri:**
- Ayarlar > Bildirimler sayfasında her bildirim türü açılıp kapatılabilir.
- Tercihler Firestore'da saklanır.
- Kapatılan bildirim türleri için FCM token'ı konudan (topic) çıkarılır.

**Öncelik:** Orta | **Aşama:** MVP

---

## 11. Reklam ve Satın Alma

### US-023 — Reklamları kaldırma satın alma

**Reklamlardan rahatsız olan bir kullanıcı olarak,** tek seferlik satın alma ile reklamları kalıcı olarak kaldırmak istiyorum.

**Kabul Kriterleri:**
- Ayarlar > "Reklamları Kaldır" seçeneği Google Play Billing akışını başlatır.
- Satın alma tokenı backend Cloud Functions üzerinden Google Play API ile doğrulanır.
- Doğrulandıktan sonra tüm reklam türleri gizlenir.
- Uygulama kaldırılıp yeniden kurulduğunda satın alma "Geri Yükle" butonu ile kurtarılabilir.

**Öncelik:** Yüksek | **Aşama:** MVP

---

## 12. Moderatör Hikayeleri

### US-024 — Gönderi moderasyonu

**Bir moderatör olarak,** onay bekleyen gönderileri inceleyerek onaylamak, reddetmek veya düzenleme istemek istiyorum, çünkü platformda yalnızca uygun içeriklerin yayımlanmasını sağlamak istiyorum.

**Kabul Kriterleri:**
- Yönetim panelinde "Onay Bekleyen Gönderiler" bölümü bulunur.
- Her gönderi için onay, red ve düzenleme iste seçenekleri bulunur.
- Red gerekçesi girilmesi zorunludur.
- İşlem sonrası gönderi sahibine bildirim gönderilir.
- Her moderasyon işlemi denetim kaydına (auditLog) yazılır.

**Öncelik:** Yüksek | **Aşama:** MVP

---

### US-025 — Şikâyet değerlendirme

**Bir moderatör olarak,** şikâyet edilen içerikleri inceleyerek gerekli işlemi yapmak istiyorum, çünkü platform güvenliğini sağlamak istiyorum.

**Kabul Kriterleri:**
- Şikâyetler listesi yönetim panelinde görünür.
- Şikâyetli içerik ve neden görüntülenebilir.
- İşlem yapıldı, reddedildi veya kapatıldı durumlarına getirilebilir.
- İşlem sonrası şikâyet sahibine bildirim gönderilir.

**Öncelik:** Yüksek | **Aşama:** MVP

---

## 13. Yönetici Hikâyeleri

### US-026 — Moderatör atama

**Bir yönetici olarak,** güvendiğim bir kullanıcıya moderatör yetkisi vermek istiyorum, çünkü platform moderasyonunu genişletmek istiyorum.

**Kabul Kriterleri:**
- Yönetim panelinde kullanıcı arama yapılabilir.
- Kullanıcıya "Moderatör Yap" işlemi uygulanabilir.
- Firebase custom claims Cloud Functions Admin SDK üzerinden güncellenir.
- Değişiklik denetim kaydına yazılır.

**Öncelik:** Yüksek | **Aşama:** MVP

---

### US-027 — Yasaklı kelime ekleme

**Bir yönetici olarak,** platforma zarar veren bir kelimeyi yasaklı kelime listesine eklemek istiyorum.

**Kabul Kriterleri:**
- Yönetim panelinde yasaklı kelimeler bölümü bulunur.
- Kelime, risk seviyesi ve açıklama girilebilir.
- Eklenen kelime anında içerik filtresinde devreye girer.
- Değişiklik denetim kaydına yazılır.

**Öncelik:** Yüksek | **Aşama:** MVP

---

### US-028 — Kullanıcı engelleme

**Bir yönetici olarak,** kural ihlali yapan bir kullanıcıyı geçici veya kalıcı olarak engellemek istiyorum.

**Kabul Kriterleri:**
- Kullanıcı yönetimi sayfasında "Engelle" seçeneği bulunur.
- Geçici engelde süre belirlenebilir.
- Kalıcı engelde kullanıcı bir daha giriş yapamaz.
- Engelleme Cloud Functions üzerinden custom claims güncellenerek gerçekleştirilir.
- Değişiklik denetim kaydına yazılır.

**Öncelik:** Yüksek | **Aşama:** MVP

---

## 14. Ayarlar ve KVKK

### US-029 — Veri indirme talebi

**Bir kullanıcı olarak,** platformdaki tüm verilerimi indirmek istiyorum, çünkü KVKK kapsamındaki veri taşınabilirliği hakkımı kullanmak istiyorum.

**Kabul Kriterleri:**
- Ayarlar > Gizlilik > "Verilerimi İndir" seçeneği bulunur.
- Talep gönderildikten sonra kullanıcıya e-posta ile bildirim yapılacağı belirtilir.
- Veri paketi hazırlandığında kullanıcıya bildirim gönderilir.

**Öncelik:** Yüksek | **Aşama:** MVP

---

### US-030 — Tema değiştirme

**Bir kullanıcı olarak,** uygulamayı koyu temada kullanmak istiyorum, çünkü gece kullanımında göz yorgunluğu yaşıyorum.

**Kabul Kriterleri:**
- Ayarlar > Uygulama bölümünde açık, koyu ve sistem teması seçenekleri bulunur.
- Seçim DataStore ile saklanır ve uygulama yeniden başlatılınca korunur.
- Değişiklik anlık uygulanır.

**Öncelik:** Orta | **Aşama:** MVP

---

*Sonraki doküman: 07-user-journeys.md*