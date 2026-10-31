# Mülkora — Kullanıcı Rolleri

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## 1. Rol Sistemi Genel Bakış

Mülkora'da 5 rol bulunmaktadır. Bir kullanıcı aynı anda hem Kiracı hem de Ev Sahibi rolünü taşıyabilir. Moderatör ve Yönetici rolleri yalnızca mevcut yönetici tarafından atanabilir; kullanıcı bu rolleri kendi hesabına ekleyemez.

Roller Firebase custom claims üzerinden yönetilir ve sunucu tarafında doğrulanır.

```
Kullanıcı Rolleri
├── Kiracı (tenant)
├── Ev Sahibi (landlord)
├── Kiracı + Ev Sahibi (both)
├── Moderatör (moderator)      → Yönetici tarafından atanır
└── Yönetici (admin)           → Sadece mevcut yönetici ekleyebilir
```

---

## 2. Kiracı (tenant)

### Kimlik ve Hesap
- Kayıt olabilir (e-posta/şifre veya Google).
- Profil oluşturabilir ve düzenleyebilir.
- Profil fotoğrafı yükleyebilir.
- Şehrini, biyografisini güncelleyebilir.
- Şifresini değiştirebilir veya sıfırlayabilir.
- Oturumunu kapatabilir.
- Hesabını silebilir (veriler KVKK uyarınca anonimleştirilir).

### Sosyal Platform
- Gönderi oluşturabilir (6 tür, 14 kategori, isteğe bağlı görsel).
- Anonim gönderi paylaşabilir.
- Gönderilerini taslak olarak kaydedebilir.
- Gönderilerini düzenleyebilir (onaylanmadan önce).
- Ana akıştaki gönderileri görüntüleyebilir.
- Gönderileri filtreleyebilir.
- Gönderi detayını görüntüleyebilir.
- Gönderi bağlantısını paylaşabilir.

### Etkileşimler
- Gönderiye beğeni verebilir.
- Gönderiye eksi oy verebilir.
- Oyunu değiştirebilir veya kaldırabilir.
- Gönderiye 1–5 yıldız puan verebilir (her gönderi için yalnızca bir kez).
- Yorum yazabilir.
- Yoruma cevap verebilir.
- Kendi yorumunu düzenleyebilir veya silebilir.
- Yorumu beğenebilir.
- Gönderi veya yorum kaydedebilir.

### Şikâyet ve Engelleme
- Gönderi, yorum, kullanıcı, referans veya referans cevabını şikâyet edebilir.
- Başka bir kullanıcıyı engelleyebilir.
- Engellediği kullanıcıları yönetebilir.

### Referans Sistemi
- Önceki veya mevcut ev sahibi hakkında referans oluşturma talebi başlatabilir.
- Hakkında referans oluşturulmak istenen kişi olarak "Evet" veya "Hayır" cevabı verebilir.
- Kendisi hakkındaki onaylı referansı görüntüleyebilir.
- Referansa cevap yazabilir.
- Referansa itiraz edebilir.

### Kira Sözleşmesi
- Sözleşme taslağı oluşturabilir.
- Şablon seçebilir, form alanlarını doldurabilir.
- İkinci tarafı (ev sahibini) davet edebilir.
- Düzenleme talebinde bulunabilir.
- Sözleşmeyi onaylayabilir.
- Onaylı sözleşmenin PDF'ini indirebilir veya paylaşabilir.
- Kendi sözleşmelerini görüntüleyebilir.

### Bildirimler ve Ayarlar
- Uygulama içi bildirimleri görüntüleyebilir.
- Bildirim tercihlerini yönetebilir.
- Tema seçebilir (açık/koyu/sistem).
- Gizlilik ayarlarını yapılandırabilir.
- Verilerini indirme talebi oluşturabilir.
- Reklam kaldırma satın alması yapabilir.
- Satın almayı geri yükleyebilir.

---

## 3. Ev Sahibi (landlord)

Ev Sahibi, Kiracı rolündeki tüm sosyal özelliklere ek olarak aşağıdaki yetkilere sahiptir:

### Ek Haklar
- Önceki veya mevcut kiracısı hakkında referans oluşturma talebi başlatabilir.
- Hakkında referans oluşturulmak istenen kişi olarak "Evet" veya "Hayır" cevabı verebilir.
- Kendisi hakkındaki onaylı referansı görüntüleyebilir.
- Referansa cevap yazabilir.
- Referansa itiraz edebilir.
- Kiracıyı sözleşmeye davet edebilir.
- Sözleşme taslaklarını yönetebilir.

---

## 4. Kiracı + Ev Sahibi (both)

- Kiracı ve Ev Sahibi rollerindeki tüm hakları aynı anda kullanabilir.
- Rol değişikliği geçmiş gönderileri, yorumları, referansları veya sözleşmeleri bozmaz.
- Profil sayfasında her iki rol rozeti gösterilir.

---

## 5. Moderatör (moderator)

Moderatör, normal kullanıcı haklarına ek olarak moderasyon yetkilerine sahiptir. Bu rol yalnızca Yönetici tarafından atanabilir.

### Moderasyon Yetkileri
- Moderasyon kuyruğundaki gönderileri görüntüleyebilir.
- Gönderileri onaylayabilir.
- Gönderileri reddedebilir (gerekçe belirterek).
- Gönderi sahibinden düzenleme talep edebilir.
- Onaylı gönderileri yayından kaldırabilir.
- Şikâyetleri görüntüleyebilir ve değerlendirebilir.
- Şikâyetli içeriklerde işlem yapabilir.
- Onay bekleyen referansları inceleyebilir.
- Referans cevaplarını inceleyebilir.
- Kullanıcılara yazılı uyarı verebilir.
- İçerikleri geçici olarak gizleyebilir.

### Moderatörün Yapamayacakları
- Kullanıcıları kalıcı engelleyemez (bu yetki Yöneticiye aittir).
- Moderatör atayamaz.
- Yasaklı kelime listesini düzenleyemez.
- Kategorileri yönetemez.
- Denetim kayıtlarını görüntüleyemez (bu yetki Yöneticiye aittir).
- Reklam ayarlarını değiştiremez.
- Sistem duyurusu gönderemez.

---

## 6. Yönetici (admin)

Yönetici, platformun en yüksek yetki seviyesine sahip rolüdür. Tüm Moderatör yetkilerini kapsar.

### Kullanıcı Yönetimi
- Kullanıcıları görüntüleyebilir, arama yapabilir.
- Kullanıcıları geçici olarak engelleyebilir.
- Kullanıcıları kalıcı olarak engelleyebilir.
- Kullanıcı engelini kaldırabilir.
- Moderatör atayabilir.
- Moderatör yetkisini kaldırabilir.

### İçerik Yönetimi
- Gönderi, yorum ve referansları silebilir.
- Tüm içerikleri görüntüleyebilir (onay bekleyenler dahil).
- Referans itirazlarını inceleyebilir ve karara bağlayabilir.

### Platform Yönetimi
- Yasaklı kelime listesini yönetebilir (ekle/düzenle/sil).
- Gönderi kategorilerini yönetebilir (ekle/düzenle/sil).
- Şikâyet nedenlerini değiştirebilir.
- Sözleşme şablonlarını yönetebilir.
- Reklam ayarlarını yönetebilir.
- Sistem duyurusu gönderebilir.
- Uygulama ayarlarını yapılandırabilir.
- Firebase Remote Config değerlerini güncelleyebilir.

### Denetim ve İzleme
- Denetim kayıtlarını (auditLogs) görüntüleyebilir.
- İstatistik panelini görüntüleyebilir.

---

## 7. Rol Atama ve Güvenlik

### Firebase Custom Claims

Roller Firebase Authentication üzerindeki custom claims ile yönetilir:

```json
{
  "role": "admin",         // "tenant" | "landlord" | "both" | "moderator" | "admin"
  "isBanned": false
}
```

- Custom claims yalnızca Cloud Functions (Admin SDK) üzerinden değiştirilebilir.
- İstemci (Android uygulaması veya Web Panel kullanıcısı) custom claims'i doğrudan değiştiremez.
- Her Cloud Functions çağrısında token'dan rol doğrulanır.
- Token yenilenmeden önce custom claims değişikliği etkili olmaz; kritik işlemler her çağrıda sunucu tarafında doğrulanır.

### Rol Kontrol Hiyerarşisi

```
isAdmin()        → role == "admin"
isModerator()    → role == "moderator" || role == "admin"
isLandlord()     → role == "landlord" || role == "both"
isTenant()       → role == "tenant" || role == "both"
isAuthenticated() → auth != null && !isBanned
```

---

## 8. Rol Değişikliğinin Etkileri

| Veri Türü | Rol Değişikliğindeki Davranış |
|---|---|
| Geçmiş gönderiler | Korunur, değişmez |
| Yorumlar | Korunur, değişmez |
| Referanslar (veren) | Korunur; referansın türü (kiracı/ev sahibi referansı) değişmez |
| Referanslar (alan) | Korunur, değişmez |
| Kira sözleşmeleri | Korunur; taraf bilgileri değişmez |
| Profil rozeti | Güncellenir; akıştaki eski gönderilerde eski rol rozeti kalır (snapshot olarak tutulur) |

**Varsayım:** Gönderi rozeti, gönderi oluşturulma anındaki rol ile birlikte saklanır. Bu, geçmiş gönderilerin doğruluğunu korur ve CLAUDE.md'deki "rol değişikliği geçmiş gönderileri bozmamalıdır" kuralına uymaktadır.

---

*Sonraki doküman: 05-permission-matrix.md*