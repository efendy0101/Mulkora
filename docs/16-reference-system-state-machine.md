# Mülkora — Referans Sistemi Durum Makinesi

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## 1. Referans Durumları

| Durum | Açıklama |
|---|---|
| `invite_sent` | Davet gönderildi, tanıma cevabı bekleniyor |
| `awaiting_confirmation` | Hedef kullanıcıya bildirim gönderildi |
| `not_recognized` | Hedef "Hayır" cevabı verdi; süreç durdu |
| `preparing` | Hedef "Evet" cevabı verdi; referans yazılıyor |
| `pending_approval` | Referans yazıldı; moderatör onayı bekleniyor |
| `approved` | Moderatör onayladı; profilde yayımlandı |
| `rejected` | Moderatör reddetti |
| `appealed` | Hedef kullanıcı itiraz etti |
| `under_review` | İtiraz inceleniyor |
| `hidden` | Moderatör geçici olarak gizledi |
| `deleted` | Yönetici sildi |

---

## 2. Durum Geçiş Diyagramı

```
[Başlangıç]
     │
     ▼
 invite_sent ──────────────────────────────────────────────────────────┐
     │                                                                  │
     │  Bildirim gönderildi                                            │
     ▼                                                                  │
awaiting_confirmation                                                   │
     │                                                                  │
     ├── Hedef "Hayır" dedi ──────────────────────────────────────────▶ not_recognized [Son]
     │
     ├── Hedef "Evet" dedi
     │
     ▼
  preparing
     │
     │  Referans yazılıp gönderildi
     ▼
pending_approval
     │
     ├── Moderatör Onayladı ──────────────────────────────────────────▶ approved
     │                                                                      │
     │                                                          ┌───────────┤
     │                                                          │           │
     │                                               Hedef     │    Moderatör
     │                                               İtiraz    │    Gizledi
     │                                               Etti      │
     │                                                ▼         ▼
     │                                            appealed    hidden
     │                                               │           │
     │                                               │ Yönetici  │ Yönetici
     │                                               │ İnceler   │ Geri Aldı
     │                                               ▼           ▼
     │                                         under_review   approved
     │                                               │
     │                                    ┌──────────┴──────────┐
     │                                    │                     │
     │                               Haklı Bulundu       Haksız Bulundu
     │                                    │                     │
     │                                    ▼                     ▼
     │                                 hidden / deleted      approved
     │
     └── Moderatör Reddetti ─────────────────────────────────────────▶ rejected [Son]

approved ──── Yönetici Siler ────────────────────────────────────────▶ deleted [Son]
hidden ───── Yönetici Siler ─────────────────────────────────────────▶ deleted [Son]
```

---

## 3. Geçiş Tablosu

| Mevcut Durum | Tetikleyici | Yeni Durum | Yapan |
|---|---|---|---|
| `invite_sent` | Sistem otomatik | `awaiting_confirmation` | Cloud Functions |
| `awaiting_confirmation` | Hedef "Hayır" | `not_recognized` | CF-REF-002 |
| `awaiting_confirmation` | Hedef "Evet" | `preparing` | CF-REF-002 |
| `preparing` | Referans gönderildi | `pending_approval` | CF-REF-003 |
| `pending_approval` | Mod. Onayladı | `approved` | CF-POST-002 variant |
| `pending_approval` | Mod. Reddetti | `rejected` | CF-POST-002 variant |
| `approved` | Hedef İtiraz Etti | `appealed` | CF-REF-006 |
| `approved` | Mod. Gizledi | `hidden` | Moderatör |
| `appealed` | Yönetici İncelemeye Aldı | `under_review` | Admin |
| `under_review` | Haklı Bulundu | `hidden` veya `deleted` | Admin |
| `under_review` | Haksız Bulundu | `approved` | Admin |
| `hidden` | Yönetici Geri Aldı | `approved` | Admin |
| `approved` / `hidden` | Yönetici Sildi | `deleted` | Admin |

---

## 4. Referans Cevabı Durum Makinesi

### Cevap Durumları

| Durum | Açıklama |
|---|---|
| `pending_approval` | Cevap gönderildi, moderatör onayı bekleniyor |
| `approved` | Onaylandı, referansın altında gösterilir |
| `rejected` | Reddedildi |
| `deleted` | Silindi |

### Cevap Geçiş Diyagramı

```
[Hedef Kullanıcı Cevap Yazar]
           │
           ▼
    pending_approval
           │
    ┌──────┴──────┐
    │             │
 Onaylandı    Reddedildi
    │             │
    ▼             ▼
 approved      rejected

approved ─── Silinir ──▶ deleted
```

---

## 5. İtiraz Durum Makinesi

### İtiraz Durumları

| Durum | Açıklama |
|---|---|
| `new` | Yeni itiraz |
| `reviewing` | Yönetici inceleniyor |
| `upheld` | İtiraz kabul edildi (referans gizlendi/silindi) |
| `rejected` | İtiraz reddedildi (referans kaldı) |
| `closed` | Kapatıldı |

### İtiraz Geçiş Diyagramı

```
Hedef İtiraz Eder
       │
       ▼
      new ─── Yönetici İncelemeye Alır ──▶ reviewing
                                               │
                                    ┌──────────┴──────────┐
                                    │                     │
                               Haklı Bulundu        Haksız Bulundu
                                    │                     │
                                    ▼                     ▼
                                 upheld               rejected
                          (Referans gizlendi/         (Referans
                              silindi)                 devam eder)
                                    │                     │
                                    └────────┬────────────┘
                                             │
                                             ▼
                                           closed
```

---

## 6. Bildirim Tetikleyicileri

| Durum Geçişi | Alıcı | Bildirim Türü |
|---|---|---|
| `invite_sent` → `awaiting_confirmation` | Hedef kullanıcı | "Bu kişiyi tanıyor musunuz?" |
| `awaiting_confirmation` → `not_recognized` | Referans yazan | "Tanımadı" |
| `awaiting_confirmation` → `preparing` | Referans yazan | "Tanıdı, yazabilirsiniz" |
| `preparing` → `pending_approval` | Hedef kullanıcı | "Hakkınızda referans incelemeye alındı" |
| `pending_approval` → `approved` | Her iki taraf | "Referans yayımlandı" |
| `pending_approval` → `rejected` | Referans yazan | "Referans reddedildi" |
| `approved` → `appealed` | Yöneticiler | "Yeni itiraz var" |
| `under_review` → `upheld` | Referans yazan | "İtiraz kabul edildi" |
| `under_review` → `rejected` | Hedef kullanıcı | "İtiraz reddedildi" |

---

## 7. İş Kuralı Korumaları

### Tekrar Referans Koruması

Aynı `authorId` ve `targetId` çifti için aynı anda yalnızca bir aktif referans olabilir. Aktif durumlar: `invite_sent`, `awaiting_confirmation`, `preparing`, `pending_approval`, `approved`, `appealed`, `under_review`, `hidden`.

### Referans Kötüye Kullanım Koruması

- Referanslar moderatör onayından geçmeden yayımlanamaz.
- Hedef kullanıcı tanımadığını belirtirse referans iptal edilir.
- Tüm referans içerikleri içerik filtresinden geçirilir.
- İtiraz sürecinde referans yayımda kalmaya devam eder (itiraz kabul edilene kadar).

### Kontrol Noktaları

```
1. initiateReference çağrıldığında:
   ✓ Kullanıcı kendi hakkında referans başlatamaz
   ✓ Hedef kullanıcı mevcut ve engellenmemiş olmalı
   ✓ Aktif referans zaten var mı?
   ✓ Rate limit: 24 saatte 5 talep

2. respondToReferenceInvite çağrıldığında:
   ✓ Cevap veren kişi targetId ile eşleşmeli
   ✓ Zaten cevap verilmemiş olmalı
   ✓ referenceId mevcut olmalı

3. submitReference çağrıldığında:
   ✓ Referansı yazan authorId ile eşleşmeli
   ✓ Status "preparing" olmalı
   ✓ Tüm zorunlu alanlar dolu olmalı
   ✓ Puan değerleri 1-5 arasında olmalı

4. submitReferenceAppeal çağrıldığında:
   ✓ İtiraz eden targetId ile eşleşmeli
   ✓ Referans "approved" durumunda olmalı
   ✓ Daha önce itiraz edilmemiş olmalı
```

---

*Sonraki doküman: 17-post-moderation-state-machine.md*