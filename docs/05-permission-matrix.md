# Mülkora — Yetki Matrisi

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## Sembol Açıklamaları

| Sembol | Anlam |
|---|---|
| ✅ | İzin var |
| ❌ | İzin yok |
| ⚠️ | Kısıtlı / Koşula bağlı |
| — | Uygulanamaz |

**Roller:**  
T = Kiracı (tenant)  
L = Ev Sahibi (landlord)  
B = Her İkisi (both = T + L)  
M = Moderatör  
A = Yönetici (admin)

---

## 1. Kimlik Doğrulama ve Hesap

| İşlem | T | L | B | M | A |
|---|---|---|---|---|---|
| E-posta/şifre ile kayıt | ✅ | ✅ | ✅ | — | — |
| Google ile giriş | ✅ | ✅ | ✅ | ✅ | ✅ |
| Şifre sıfırlama | ✅ | ✅ | ✅ | ✅ | ✅ |
| E-posta doğrulama | ✅ | ✅ | ✅ | ✅ | ✅ |
| Oturumu kapatma | ✅ | ✅ | ✅ | ✅ | ✅ |
| Kendi hesabını silme | ✅ | ✅ | ✅ | ✅ | ✅ |
| Başka hesabı silme | ❌ | ❌ | ❌ | ❌ | ✅ |
| Kendi rolünü admin/mod yapmak | ❌ | ❌ | ❌ | ❌ | ❌ |
| Moderatör atamak | ❌ | ❌ | ❌ | ❌ | ✅ |
| Moderatör yetkisini kaldırmak | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## 2. Profil

| İşlem | T | L | B | M | A |
|---|---|---|---|---|---|
| Kendi profilini görüntülemek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Başka kullanıcı profilini görüntülemek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Kendi profilini düzenlemek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Başka kullanıcının profilini düzenlemek | ❌ | ❌ | ❌ | ❌ | ❌ |
| Profil fotoğrafı yüklemek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Profil görünürlüğünü ayarlamak | ✅ | ✅ | ✅ | ✅ | ✅ |
| Kullanıcıyı şikâyet etmek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Kullanıcıyı engellemek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Kullanıcıyı geçici engellemek (platform) | ❌ | ❌ | ❌ | ❌ | ✅ |
| Kullanıcıyı kalıcı engellemek (platform) | ❌ | ❌ | ❌ | ❌ | ✅ |
| Kullanıcı engeline uyarı vermek | ❌ | ❌ | ❌ | ✅ | ✅ |
| Veri indirme talebi oluşturmak | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 3. Gönderi Oluşturma

| İşlem | T | L | B | M | A |
|---|---|---|---|---|---|
| Gönderi oluşturmak | ✅ | ✅ | ✅ | ✅ | ✅ |
| Taslak kaydetmek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Anonim gönderi oluşturmak | ✅ | ✅ | ✅ | ✅ | ✅ |
| Görsel eklemek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Yorum izni kapatmak | ✅ | ✅ | ✅ | ✅ | ✅ |
| Kendi gönderisini düzenlemek (onaydan önce) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Kendi gönderisini silmek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Başka kullanıcının gönderisini silmek | ❌ | ❌ | ❌ | ❌ | ✅ |
| Kendi gönderisini onaylamak | ❌ | ❌ | ❌ | ❌ | ❌ |

---

## 4. Ana Akış ve Gönderi Görüntüleme

| İşlem | T | L | B | M | A |
|---|---|---|---|---|---|
| Onaylı gönderileri görmek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Onay bekleyen gönderileri görmek (kendi) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Onay bekleyen gönderileri görmek (hepsi) | ❌ | ❌ | ❌ | ✅ | ✅ |
| Reddedilen gönderileri görmek (kendi) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Reddedilen gönderileri görmek (hepsi) | ❌ | ❌ | ❌ | ✅ | ✅ |
| Akışı filtrelemek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Gönderi detayını görüntülemek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Gönderi bağlantısını paylaşmak | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 5. Moderasyon Kuyruğu

| İşlem | T | L | B | M | A |
|---|---|---|---|---|---|
| Moderasyon kuyruğunu görüntülemek | ❌ | ❌ | ❌ | ✅ | ✅ |
| Gönderiyi onaylamak | ❌ | ❌ | ❌ | ✅ | ✅ |
| Gönderiyi reddetmek | ❌ | ❌ | ❌ | ✅ | ✅ |
| Düzenleme talep etmek | ❌ | ❌ | ❌ | ✅ | ✅ |
| Gönderiyi yayından kaldırmak | ❌ | ❌ | ❌ | ✅ | ✅ |
| İçeriği geçici gizlemek | ❌ | ❌ | ❌ | ✅ | ✅ |
| Moderasyon gerekçesi kaydetmek | ❌ | ❌ | ❌ | ✅ | ✅ |

---

## 6. Etkileşimler (Beğeni, Eksi Oy, Puan)

| İşlem | T | L | B | M | A |
|---|---|---|---|---|---|
| Gönderiye beğeni vermek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Gönderiye eksi oy vermek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Aynı gönderiye beğeni + eksi oy vermek | ❌ | ❌ | ❌ | ❌ | ❌ |
| Oyunu kaldırmak | ✅ | ✅ | ✅ | ✅ | ✅ |
| Oy türünü değiştirmek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Gönderiye puan vermek (1–5) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Aynı gönderiye ikinci kez puan vermek | ❌ | ❌ | ❌ | ❌ | ❌ |
| Başkası adına oy vermek | ❌ | ❌ | ❌ | ❌ | ❌ |
| Sayaçları doğrudan Firestore'dan değiştirmek | ❌ | ❌ | ❌ | ❌ | ❌ |
| Gönderi kaydetmek | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 7. Yorum ve Cevap

| İşlem | T | L | B | M | A |
|---|---|---|---|---|---|
| Yorum yazmak | ✅ | ✅ | ✅ | ✅ | ✅ |
| Yoruma cevap yazmak | ✅ | ✅ | ✅ | ✅ | ✅ |
| Kendi yorumunu düzenlemek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Kendi yorumunu silmek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Başkasının yorumunu silmek | ❌ | ❌ | ❌ | ❌ | ✅ |
| Yorumu beğenmek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Yorumu şikâyet etmek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Cevapları açıp kapatmak | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 8. Şikâyet Sistemi

| İşlem | T | L | B | M | A |
|---|---|---|---|---|---|
| İçerik şikâyet etmek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Aynı içeriği tekrar şikâyet etmek | ❌ | ❌ | ❌ | ❌ | ❌ |
| Şikâyetleri görüntülemek (kendi) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Tüm şikâyetleri görüntülemek | ❌ | ❌ | ❌ | ✅ | ✅ |
| Şikâyeti değerlendirmek | ❌ | ❌ | ❌ | ✅ | ✅ |
| Şikâyet nedenlerini düzenlemek | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## 9. Referans Sistemi

| İşlem | T | L | B | M | A |
|---|---|---|---|---|---|
| Ev sahibi hakkında referans başlatmak | ✅ | — | ✅ | ❌ | ❌ |
| Kiracı hakkında referans başlatmak | — | ✅ | ✅ | ❌ | ❌ |
| Tanıma sorusuna cevap vermek ("Evet"/"Hayır") | ✅ | ✅ | ✅ | ❌ | ❌ |
| Referans yazmak (evet cevabı sonrası) | ✅ | ✅ | ✅ | ❌ | ❌ |
| Başkası adına referans yazmak | ❌ | ❌ | ❌ | ❌ | ❌ |
| Kendi hakkındaki referansı görüntülemek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Referansa cevap yazmak | ✅ | ✅ | ✅ | ❌ | ❌ |
| Başkası adına referans cevabı yazmak | ❌ | ❌ | ❌ | ❌ | ❌ |
| Referansa itiraz etmek | ✅ | ✅ | ✅ | ❌ | ❌ |
| Referansı onaylamak / reddetmek | ❌ | ❌ | ❌ | ✅ | ✅ |
| Referans itirazını incelemek | ❌ | ❌ | ❌ | ✅ | ✅ |
| Referansı silmek | ❌ | ❌ | ❌ | ❌ | ✅ |
| Referansı şikâyet etmek | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 10. Kira Sözleşmesi

| İşlem | T | L | B | M | A |
|---|---|---|---|---|---|
| Sözleşme taslağı oluşturmak | ✅ | ✅ | ✅ | ❌ | ❌ |
| İkinci tarafı davet etmek | ✅ | ✅ | ✅ | ❌ | ❌ |
| Sözleşmeyi incelemek (taraf olarak) | ✅ | ✅ | ✅ | ❌ | ❌ |
| Düzenleme talebinde bulunmak | ✅ | ✅ | ✅ | ❌ | ❌ |
| Sözleşmeyi onaylamak (taraf olarak) | ✅ | ✅ | ✅ | ❌ | ❌ |
| Sözleşmeyi görüntülemek (taraf değilken) | ❌ | ❌ | ❌ | ❌ | ✅ |
| PDF indirmek / paylaşmak | ✅ | ✅ | ✅ | ❌ | ✅ |
| Sözleşme şablonlarını yönetmek | ❌ | ❌ | ❌ | ❌ | ✅ |
| Kilitli sözleşmeyi değiştirmek | ❌ | ❌ | ❌ | ❌ | ❌ |

---

## 11. Bildirimler

| İşlem | T | L | B | M | A |
|---|---|---|---|---|---|
| Kendi bildirimlerini görüntülemek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Bildirim tercihlerini yönetmek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Sistem duyurusu göndermek | ❌ | ❌ | ❌ | ❌ | ✅ |
| Push bildirim göndermek (tüm kullanıcılara) | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## 12. İçerik Filtresi

| İşlem | T | L | B | M | A |
|---|---|---|---|---|---|
| Yasaklı kelime listesini görüntülemek | ❌ | ❌ | ❌ | ❌ | ✅ |
| Yasaklı kelime eklemek | ❌ | ❌ | ❌ | ❌ | ✅ |
| Yasaklı kelimeyi düzenlemek / silmek | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## 13. Kategori Yönetimi

| İşlem | T | L | B | M | A |
|---|---|---|---|---|---|
| Kategorileri görüntülemek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Kategori eklemek | ❌ | ❌ | ❌ | ❌ | ✅ |
| Kategori düzenlemek / silmek | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## 14. Reklam ve Satın Alma

| İşlem | T | L | B | M | A |
|---|---|---|---|---|---|
| Reklamları görmek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Reklam kaldırma satın almak | ✅ | ✅ | ✅ | ✅ | ✅ |
| Satın almayı geri yüklemek | ✅ | ✅ | ✅ | ✅ | ✅ |
| Reklam ayarlarını yönetmek | ❌ | ❌ | ❌ | ❌ | ✅ |
| Satın alma tokenını doğrulamak (istemciden) | ❌ | ❌ | ❌ | ❌ | ❌ |

---

## 15. Yönetim Paneli

| İşlem | T | L | B | M | A |
|---|---|---|---|---|---|
| Yönetim paneline erişmek | ❌ | ❌ | ❌ | ✅ | ✅ |
| Gösterge panelini görmek | ❌ | ❌ | ❌ | ✅ | ✅ |
| Kullanıcı yönetimi panelini görmek | ❌ | ❌ | ❌ | ❌ | ✅ |
| İstatistik panelini görmek | ❌ | ❌ | ❌ | ✅ | ✅ |
| Denetim kayıtlarını görmek | ❌ | ❌ | ❌ | ❌ | ✅ |
| Sistem ayarlarını değiştirmek | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## 16. Denetim Kayıtları (auditLogs)

| İşlem | T | L | B | M | A |
|---|---|---|---|---|---|
| Denetim kaydı oluşturmak (otomatik) | — | — | — | ✅ | ✅ |
| Denetim kayıtlarını okumak | ❌ | ❌ | ❌ | ❌ | ✅ |
| Denetim kayıtlarını silmek | ❌ | ❌ | ❌ | ❌ | ❌ |

> Denetim kayıtları hiçbir kullanıcı tarafından silinemez; yalnızca sistem saklama politikasına göre arşivlenir.

---

## 17. Güvenlik Kuralı Özeti

Tüm iznler Firestore Security Rules ve Cloud Functions üzerinde zorunlu kılınır. İstemci tarafındaki kontroller yalnızca kullanıcı deneyimi amacıyla gösterim/gizleme yapmaya yöneliktir; güvenlik kaynağı değildir.

```
Temel Kural: Hiçbir istemci, sunucu tarafı doğrulama olmadan izin kapsamı dışında bir işlem yapamaz.
```

---

*Sonraki doküman: 06-user-stories.md*