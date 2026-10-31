# Mülkora — Firestore Security Rules Testleri

## Gereksinimler

- Node.js 20+
- Firebase CLI (`npm install -g firebase-tools`)
- Firebase Emulator Suite

## Kurulum

```bash
cd tests
npm install
```

## Testleri Çalıştırma

### 1. Emulator'ü başlat

Proje kök dizininde:

```bash
firebase emulators:start --only firestore,auth
```

### 2. Testleri çalıştır (ayrı terminal)

```bash
cd tests
npm run test:rules
```

### Cloud Functions (contentFilter) testleri

```bash
cd tests
npm run test:functions
```

## Test Kapsamı

### Güvenlik Kuralları (`security-rules/`)

- `users.test.ts` — Profil okuma (isAuthenticated), yazma koruma (role/adsRemoved/postCount), username write:if false
- `posts.test.ts` — Gönderi durumları (pending/approved/rejected), sayaç koruma, create+update:if false, delete:isAdmin
- `references.test.ts` — Referans erişim kontrolü (isParty), taraf doğrulama, write:if false
- `contracts.test.ts` — Sözleşme taraf erişim kontrolü, durum geçişleri
- `comments.test.ts` — Yorum okuma (isAuthenticated), yazma yasakları (write: if false)
- `reactions.test.ts` — Tepki okuma (isAuthenticated), yazma yasakları (write: if false)
- `notifications.test.ts` — Bildirim sahiplik kontrolü (isOwner), isRead güncelleme
- `reports.test.ts` — Şikayet okuma (reporter/moderatör), moderatör güncelleme, admin silme
- `purchases.test.ts` — Satın alma sahiplik kontrolü (isOwner), yazma yasakları (write: if false)
- `dataExportRequests.test.ts` — Veri indirme talebi oluşturma ve okuma kuralları
- `blockedUsers.test.ts` — Engelleme kaydı okuma (blockerUid), yazma yasakları (write: if false)
- `postRatings.test.ts` — Gönderi puan kaydı okuma (isAuthenticated), yazma yasakları (write: if false)
- `commentReactions.test.ts` — Yorum tepki kaydı okuma (isAuthenticated), yazma yasakları (write: if false)

### Cloud Functions Birimleri (`functions/`)

- `contentFilter.test.ts` — İçerik filtresi analiz mantığı, yasaklı kelime tespiti

## Mimari Notlar

- Tüm **yazma işlemleri** (post oluşturma, yorum ekleme, tepki verme vb.) Cloud Functions
  Admin SDK üzerinden yapılır. İstemci tarafı yazma güvenlik kurallarıyla engellenir.
- `write: if false` kuralı olan koleksiyonlar: `postReactions`, `posts/comments`,
  `purchases`, `contracts` (kısmi).
- `create: if false` kuralı olan koleksiyonlar: `reports`, `notifications`.
- Her test dosyası, `helpers.ts` içindeki yardımcı fonksiyonları kullanır:
  `authenticatedContext`, `unauthenticatedContext`, `adminContext`,
  `moderatorContext`, `bannedUserContext`.

## CLAUDE.md Referansları

- §26: Firestore ve Storage Güvenliği
- §32: Test Senaryoları listesi
- docs/15-security-rules-approach.md — Güvenlik kuralı tasarım yaklaşımı