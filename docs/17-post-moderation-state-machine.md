# Mülkora — Gönderi Moderasyon Durum Makinesi

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## 1. Gönderi Durumları

| Durum | Açıklama |
|---|---|
| `draft` | Taslak — kullanıcı henüz göndermedi |
| `pending` | İnceleme bekliyor — içerik filtresi geçti |
| `filtered` | Otomatik filtre tetiklendi — orta risk |
| `under_review` | Moderatör inceliyor |
| `edit_requested` | Düzenleme istendi — kullanıcı düzenleyebilir |
| `approved` | Onaylandı — akışta görünür |
| `rejected` | Reddedildi |
| `unpublished` | Yayından kaldırıldı |
| `deleted` | Silindi |

---

## 2. Durum Geçiş Diyagramı

```
[Kullanıcı "Paylaş" butonuna basar]
              │
              ▼
    ┌─── İçerik Filtresi ───┐
    │                       │
    │  Risk Düşük           │  Risk Orta
    │    ▼                  │    ▼
    │  pending           filtered
    │                       │
    └───────────────────────┘
              │
              ▼
        Moderasyon Kuyruğuna Alındı
              │
              ▼
        under_review (Moderatör işlem yapıyor)
              │
    ┌─────────┼─────────────────────────────────┐
    │         │                                 │
    ▼         ▼                                 ▼
approved   rejected                     edit_requested
    │                                          │
    │                                          │  Kullanıcı
    │                                          │  düzenler ve
    │                                          │  yeniden gönderir
    │                                          ▼
    │                                        pending
    │                                    (döngü başa döner)
    │
    ├── Moderatör Yayından Kaldırır ──▶ unpublished
    │                                          │
    │                                          │ Moderatör
    │                                          │ Geri Aldı
    │                                          ▼
    │                                        approved
    │
    └── Yönetici Siler ──────────────▶ deleted [Son]

rejected ──── Yönetici Siler ────────▶ deleted [Son]

draft ──── Kullanıcı Gönderir ───────▶ pending
draft ──── Kullanıcı Siler ──────────▶ deleted [Son]

[Risk Yüksek / Kritik]
    │
    ▼
 Gönderi reddedilir (kullanıcıya hata mesajı)
 Kritik → Yöneticiye otomatik bildirim
```

---

## 3. Geçiş Tablosu

| Mevcut Durum | Tetikleyici | Yeni Durum | Yapan |
|---|---|---|---|
| `draft` | Kullanıcı gönderir | `pending` veya `filtered` | İçerik filtresi |
| `draft` | Kullanıcı siler | `deleted` | Kullanıcı / Cloud Functions |
| `pending` | Moderatör Onaylar | `approved` | Moderatör |
| `pending` | Moderatör Reddeder | `rejected` | Moderatör |
| `pending` | Mod. Düzenleme İster | `edit_requested` | Moderatör |
| `filtered` | Moderatör Onaylar | `approved` | Moderatör |
| `filtered` | Moderatör Reddeder | `rejected` | Moderatör |
| `filtered` | Mod. Düzenleme İster | `edit_requested` | Moderatör |
| `edit_requested` | Kullanıcı Düzenler | `pending` | Cloud Functions |
| `edit_requested` | Kullanıcı Siler | `deleted` | Cloud Functions |
| `approved` | Mod. Yayından Kaldırır | `unpublished` | Moderatör |
| `unpublished` | Mod. Geri Alır | `approved` | Moderatör |
| `approved` | Yönetici Siler | `deleted` | Admin |
| `rejected` | Yönetici Siler | `deleted` | Admin |
| `unpublished` | Yönetici Siler | `deleted` | Admin |

---

## 4. Risk Seviyesi → Durum Eşlemesi

| Risk Seviyesi | İçerik Filtresi Sonucu | Kullanıcı Mesajı |
|---|---|---|
| `low` | `pending` oluşturulur | "Uyarı: İçerik dikkat çekti. Devam etmek ister misiniz?" |
| `medium` | `filtered` oluşturulur | "Gönderiniz incelemeye alındı." |
| `high` | Gönderi oluşturulmaz | "Gönderiniz kurallara aykırı içerik içeriyor." |
| `critical` | Gönderi oluşturulmaz, yönetici bildirilir | "İçeriğiniz yayımlanamaz." |

---

## 5. Moderasyon Bildirim Tetikleyicileri

| Durum Geçişi | Alıcı | Bildirim Türü |
|---|---|---|
| `pending/filtered` oluştu | Moderatörler | "Yeni gönderi moderasyon bekliyor" |
| `pending` → `approved` | Gönderi sahibi | "Gönderiniz yayımlandı" |
| `pending` → `rejected` | Gönderi sahibi | "Gönderiniz reddedildi — [gerekçe]" |
| `pending` → `edit_requested` | Gönderi sahibi | "Gönderinizde düzenleme gerekiyor" |
| `approved` → `unpublished` | Gönderi sahibi | "Gönderiniz yayından kaldırıldı" |

---

## 6. Yorum Durum Makinesi

### Yorum Durumları

| Durum | Açıklama |
|---|---|
| `visible` | Görünür |
| `hidden` | Moderatör tarafından gizlendi |
| `deleted` | Silindi (soft delete) |

### Yorum Geçiş Diyagramı

```
[Kullanıcı Yorum Gönderir]
           │
     İçerik Filtresi
           │
    ┌──────┴──────────────┐
    │                     │
 Risk Düşük/Orta      Risk Yüksek
    │                     │
    ▼                     ▼
 visible              hidden (moderasyon)
    │                     │
    ├── Mod. Gizledi       │ Mod. Onaylarsa
    │       ▼             ▼
    │    hidden         visible
    │       │
    │  Mod. Geri Alır
    │       ▼
    │    visible
    │
    ├── Kullanıcı Siler ──▶ deleted [Son]
    └── Mod. Siler ────────▶ deleted [Son]
```

---

## 7. State Machine Kotlin Implementasyonu (Örnek)

```kotlin
enum class PostStatus {
    DRAFT,
    PENDING,
    FILTERED,
    UNDER_REVIEW,
    EDIT_REQUESTED,
    APPROVED,
    REJECTED,
    UNPUBLISHED,
    DELETED
}

object PostStateMachine {

    fun isValidTransition(from: PostStatus, to: PostStatus): Boolean {
        return when (from) {
            PostStatus.DRAFT -> to in listOf(
                PostStatus.PENDING, PostStatus.FILTERED, PostStatus.DELETED
            )
            PostStatus.PENDING, PostStatus.FILTERED -> to in listOf(
                PostStatus.APPROVED, PostStatus.REJECTED, PostStatus.EDIT_REQUESTED
            )
            PostStatus.EDIT_REQUESTED -> to in listOf(
                PostStatus.PENDING, PostStatus.DELETED
            )
            PostStatus.APPROVED -> to in listOf(
                PostStatus.UNPUBLISHED, PostStatus.DELETED
            )
            PostStatus.UNPUBLISHED -> to in listOf(
                PostStatus.APPROVED, PostStatus.DELETED
            )
            PostStatus.REJECTED -> to == PostStatus.DELETED
            else -> false
        }
    }
}
```

---

## 8. Sözleşme Durum Makinesi

### Sözleşme Durumları

| Durum | Açıklama |
|---|---|
| `draft` | Taslak — sadece oluşturan görebilir |
| `sent` | İkinci tarafa gönderildi |
| `under_review` | İkinci taraf inceliyor |
| `edit_requested` | Düzenleme talep edildi |
| `approved` | Her iki taraf onayladı |
| `locked` | Kilitlendi, değiştirilemez |
| `pdf_ready` | PDF hazırlandı |

### Sözleşme Geçiş Diyagramı

```
draft ─── Gönderildi ──▶ sent
sent ──── İnceleniyor ─▶ under_review
          │
    ┌─────┴──────────────────────┐
    │                            │
    │ Düzenleme İstendi     Her İki Taraf
    ▼                       Onayladı
edit_requested               │
    │                         ▼
    │ Düzenlendi           approved
    ▼                         │
under_review                  ▼
    (döngü)                locked
                              │
                              ▼
                           pdf_ready [Son — değiştirilemez]
```

---

*Sonraki doküman: 18-content-filter-design.md*