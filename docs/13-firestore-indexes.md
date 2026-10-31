# Mülkora — Gerekli Firestore İndeksleri

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## Genel Notlar

- Firestore, tek alanlı sorgular için otomatik indeks oluşturur.
- Birden fazla alan içeren sorgular için bileşik (composite) indeks gerekir.
- Bu liste `firestore.indexes.json` dosyasına eklenmelidir.
- `ASC` = artan, `DESC` = azalan sıralama.

---

## 1. `posts` Koleksiyonu

### IDX-001: Ana akış — onaylı gönderiler, tarihe göre sıralı

```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

**Sorgu:** `where("status", "==", "approved").orderBy("createdAt", "DESC")`

---

### IDX-002: Ana akış — en çok beğenilen

```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "likeCount", "order": "DESCENDING" }
  ]
}
```

---

### IDX-003: Ana akış — en çok yorumlanan

```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "commentCount", "order": "DESCENDING" }
  ]
}
```

---

### IDX-004: Ana akış — en yüksek puanlı

```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "averageRating", "order": "DESCENDING" }
  ]
}
```

---

### IDX-005: Rolle filtre — kiracı gönderileri, en yeni

```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "authorRole", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

**Sorgu:** `where("status", "==", "approved").where("authorRole", "==", "tenant").orderBy("createdAt", "DESC")`

---

### IDX-006: Şehir bazlı filtre, en yeni

```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "city", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

---

### IDX-007: Kategori bazlı filtre, en yeni

```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "category", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

---

### IDX-008: Gönderi türü filtresi, en yeni

```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "postType", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

---

### IDX-009: Kullanıcının kendi gönderileri (profil sayfası)

```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "authorId", "order": "ASCENDING" },
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

---

### IDX-010: Moderasyon kuyruğu — bekleyen gönderiler, riske göre öncelikli

```json
{
  "collectionGroup": "posts",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "filterRiskLevel", "order": "DESCENDING" },
    { "fieldPath": "createdAt", "order": "ASCENDING" }
  ]
}
```

---

## 2. `comments` Koleksiyonu

### IDX-011: Gönderi yorumları — görünür, tarihe göre

```json
{
  "collectionGroup": "comments",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "postId", "order": "ASCENDING" },
    { "fieldPath": "parentCommentId", "order": "ASCENDING" },
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "ASCENDING" }
  ]
}
```

**Sorgu:** `where("postId", "==", id).where("parentCommentId", "==", null).where("status", "==", "visible").orderBy("createdAt", "ASC")`

---

### IDX-012: Bir yorumun cevapları

```json
{
  "collectionGroup": "comments",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "parentCommentId", "order": "ASCENDING" },
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "ASCENDING" }
  ]
}
```

---

### IDX-013: Kullanıcının yorumları (profil sayfası)

```json
{
  "collectionGroup": "comments",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "authorId", "order": "ASCENDING" },
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

---

## 3. `reports` Koleksiyonu

### IDX-014: Şikâyetler — türe ve duruma göre

```json
{
  "collectionGroup": "reports",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "targetType", "order": "ASCENDING" },
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

---

### IDX-015: Belirli içeriğe yapılan şikâyetler (tekrar kontrolü)

```json
{
  "collectionGroup": "reports",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "reporterId", "order": "ASCENDING" },
    { "fieldPath": "targetId", "order": "ASCENDING" },
    { "fieldPath": "targetType", "order": "ASCENDING" }
  ]
}
```

---

## 4. `references` Koleksiyonu

### IDX-016: Bir kullanıcının aldığı onaylı referanslar

```json
{
  "collectionGroup": "references",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "targetId", "order": "ASCENDING" },
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

---

### IDX-017: Bir kullanıcının verdiği referanslar

```json
{
  "collectionGroup": "references",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "authorId", "order": "ASCENDING" },
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

---

### IDX-018: Moderasyon — bekleyen referanslar

```json
{
  "collectionGroup": "references",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "ASCENDING" }
  ]
}
```

---

## 5. `referenceResponses` Koleksiyonu

### IDX-019: Bir referansın cevabı

```json
{
  "collectionGroup": "referenceResponses",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "referenceId", "order": "ASCENDING" },
    { "fieldPath": "status", "order": "ASCENDING" }
  ]
}
```

---

## 6. `contracts` Koleksiyonu

### IDX-020: Kullanıcının sözleşmeleri (ev sahibi)

```json
{
  "collectionGroup": "contracts",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "landlordId", "order": "ASCENDING" },
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

---

### IDX-021: Kullanıcının sözleşmeleri (kiracı)

```json
{
  "collectionGroup": "contracts",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "tenantId", "order": "ASCENDING" },
    { "fieldPath": "status", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

---

## 7. `notifications` Koleksiyonu

### IDX-022: Kullanıcının bildirimleri — okunmamış önce

```json
{
  "collectionGroup": "notifications",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "recipientId", "order": "ASCENDING" },
    { "fieldPath": "isRead", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

---

### IDX-023: Kullanıcının bildirimleri — tarihe göre

```json
{
  "collectionGroup": "notifications",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "recipientId", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

---

## 8. `savedPosts` Koleksiyonu

### IDX-024: Kullanıcının kaydettiği gönderiler, tarihe göre

```json
{
  "collectionGroup": "savedPosts",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "userId", "order": "ASCENDING" },
    { "fieldPath": "savedAt", "order": "DESCENDING" }
  ]
}
```

---

## 9. `auditLogs` Koleksiyonu

### IDX-025: Denetim kayıtları — tarihe göre

```json
{
  "collectionGroup": "auditLogs",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "adminId", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

---

### IDX-026: Belirli içerik için denetim kayıtları

```json
{
  "collectionGroup": "auditLogs",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "targetType", "order": "ASCENDING" },
    { "fieldPath": "targetId", "order": "ASCENDING" },
    { "fieldPath": "createdAt", "order": "DESCENDING" }
  ]
}
```

---

## 10. `blockedUsers` Koleksiyonu

### IDX-027: Kullanıcının engellediği kişiler

```json
{
  "collectionGroup": "blockedUsers",
  "queryScope": "COLLECTION",
  "fields": [
    { "fieldPath": "blockerId", "order": "ASCENDING" },
    { "fieldPath": "blockedAt", "order": "DESCENDING" }
  ]
}
```

---

## `firestore.indexes.json` Özet Yapısı

```json
{
  "indexes": [
    // IDX-001 — IDX-027 buraya eklenir
  ],
  "fieldOverrides": [
    {
      "collectionGroup": "auditLogs",
      "fieldPath": "__name__",
      "indexes": []
    }
  ]
}
```

**Not:** `fieldOverrides` ile `auditLogs` koleksiyonu için gereksiz otomatik indeksler devre dışı bırakılarak maliyet optimize edilir.

---

## İndeks Önceliklendirme

| Öncelik | İndeks | Gerekçe |
|---|---|---|
| Kritik | IDX-001 | Ana akış her kullanıcı için çalışır |
| Kritik | IDX-009 | Profil sayfası sık açılır |
| Kritik | IDX-011 | Gönderi detayı her açılışta çalışır |
| Kritik | IDX-022 | Bildirim merkezi |
| Yüksek | IDX-010 | Moderasyon iş akışı |
| Yüksek | IDX-016 | Referans profil gösterimi |
| Yüksek | IDX-020, IDX-021 | Sözleşme listesi |
| Orta | IDX-002 – IDX-008 | Filtreleme seçenekleri |
| Düşük | IDX-025, IDX-026 | Yönetici denetim paneli |

---

*Sonraki doküman: 14-cloud-functions-architecture.md*