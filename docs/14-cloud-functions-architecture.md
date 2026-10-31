# Mülkora — Cloud Functions Mimarisi

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## 1. Teknoloji ve Yapı

- **Runtime:** Node.js 20 + TypeScript
- **Firebase SDK:** Admin SDK v12+
- **Fonksiyon türleri:** `onCall` (HTTPS Callable), `onRequest` (HTTPS), `onSchedule` (Scheduler), `onDocumentCreated/Updated` (Firestore triggers)
- **Dağıtım:** Firebase Functions v2 (2. nesil, Cloud Run tabanlı)
- **Test:** Jest + Firebase Emulator Suite

### Dizin Yapısı

```
functions/
├── src/
│   ├── auth/
│   │   ├── createUserProfile.ts
│   │   ├── checkUsername.ts
│   │   ├── reserveUsername.ts
│   │   └── deleteAccount.ts
│   ├── posts/
│   │   ├── submitPost.ts
│   │   ├── updatePost.ts
│   │   ├── moderatePost.ts
│   │   └── contentFilter.ts
│   ├── reactions/
│   │   ├── toggleReaction.ts
│   │   └── ratePost.ts
│   ├── comments/
│   │   ├── addComment.ts
│   │   ├── editComment.ts
│   │   └── deleteComment.ts
│   ├── reports/
│   │   └── createReport.ts
│   ├── references/
│   │   ├── initiateReference.ts
│   │   ├── respondToReferenceInvite.ts
│   │   ├── submitReference.ts
│   │   ├── moderateReference.ts
│   │   ├── submitReferenceResponse.ts
│   │   └── submitReferenceAppeal.ts
│   ├── contracts/
│   │   ├── createContract.ts
│   │   ├── inviteContractParty.ts
│   │   ├── requestContractEdit.ts
│   │   ├── approveContract.ts
│   │   ├── lockContractVersion.ts
│   │   └── generateContractPdf.ts
│   ├── notifications/
│   │   └── sendPushNotification.ts
│   ├── admin/
│   │   ├── setUserRole.ts
│   │   ├── banUser.ts
│   │   ├── unbanUser.ts
│   │   └── createAuditLog.ts
│   ├── billing/
│   │   ├── verifyPurchase.ts
│   │   ├── restorePurchase.ts
│   │   └── handleRefund.ts
│   ├── scheduled/
│   │   ├── cleanupNotifications.ts
│   │   ├── cleanupModerationQueue.ts
│   │   └── checkBanExpiry.ts
│   ├── triggers/
│   │   ├── onPostStatusChange.ts
│   │   └── onReferenceStatusChange.ts
│   ├── middleware/
│   │   ├── authMiddleware.ts       (Authentication kontrolü)
│   │   ├── appCheckMiddleware.ts   (App Check kontrolü)
│   │   ├── roleMiddleware.ts       (Rol kontrolü)
│   │   ├── rateLimitMiddleware.ts  (Rate limiting)
│   │   └── validationMiddleware.ts (Veri doğrulama)
│   └── util/
│       ├── contentFilter.ts
│       ├── pdfGenerator.ts
│       ├── hashUtil.ts
│       └── idGenerator.ts
├── test/
│   ├── auth.test.ts
│   ├── posts.test.ts
│   ├── references.test.ts
│   └── contracts.test.ts
├── package.json
└── tsconfig.json
```

---

## 2. Ortak Middleware Katmanı

Tüm `onCall` fonksiyonlar aşağıdaki middleware'i sırayla uygular:

```typescript
// Her callable fonksiyonda zorunlu kontrol sırası:
// 1. App Check doğrulama
// 2. Authentication doğrulama (auth != null)
// 3. Engel kontrolü (isBanned == false)
// 4. Rol kontrolü (fonksiyona göre)
// 5. Rate limiting
// 6. Veri doğrulama (zod / manuel)
// 7. İş mantığı
// 8. Audit log (admin/moderatör işlemleri)
```

---

## 3. Kimlik Doğrulama Fonksiyonları

### CF-AUTH-001: `createUserProfile`

**Tür:** onCall  
**Tetikleyici:** Kayıt sonrası çağrılır (Firebase Auth oluşumundan sonra)  
**Gereklilik:** Sadece authentication (yeni hesap)

**Giriş:**
```typescript
{
  username: string;       // 3-30 karakter, harf/rakam/alt çizgi
  displayName: string;    // 2-50 karakter
  role: "tenant" | "landlord" | "both";
  city: string;
  bio?: string;
  termsAccepted: boolean;
  kvkkAccepted: boolean;
  ageConfirmed: boolean;
}
```

**İşlem Adımları:**
1. App Check kontrolü
2. Auth token doğrulama
3. Giriş verisi doğrulama
4. `usernames/{username}` belgesi mevcut mu? → Hata fırlat
5. Firestore transaction başlat:
   - `usernames/{username}` → `{ uid, reservedAt }` oluştur
   - `users/{uid}` → profil belgesi oluştur
6. FCM topic'e abone ol
7. Başarı döndür

**Hata Kodları:** `username-taken`, `invalid-input`, `already-exists`

---

### CF-AUTH-002: `checkUsername`

**Tür:** onCall  
**Gereklilik:** Opsiyonel authentication

**Giriş:** `{ username: string }`  
**Çıkış:** `{ available: boolean }`  
**İşlem:** `usernames/{username}` belgesinin varlığını kontrol eder.

---

### CF-AUTH-003: `deleteAccount`

**Tür:** onCall  
**Gereklilik:** Authentication (kendi hesabı)

**İşlem Adımları:**
1. Auth token doğrulama
2. Firestore batch işlemi:
   - `users/{uid}` → kişisel alanları temizle, `deletedAt` doldur
   - `usernames/{username}` → belgeyi sil (başkası kullanabilsin)
   - `notifications/{uid}` sorgula → hepsini sil
   - `fcmToken` temizle
3. `posts` koleksiyonunda `authorId == uid` → `authorUsername`, `authorDisplayName`, `authorAvatarUrl` anonimleştir
4. `comments` koleksiyonunda `authorId == uid` → anonimleştir
5. Storage'dan profil fotoğrafını sil
6. Firebase Auth hesabını sil (Admin SDK)

---

## 4. Gönderi Fonksiyonları

### CF-POST-001: `submitPost`

**Tür:** onCall  
**Gereklilik:** Authentication + e-posta doğrulaması + engel kontrolü

**Giriş:**
```typescript
{
  postType: string;
  category: string;
  title: string;         // max 200 karakter
  content: string;       // max 5000 karakter
  imageUrl?: string;     // Zaten Storage'a yüklenmiş URL
  city: string;
  isAnonymous: boolean;
  commentsAllowed: boolean;
}
```

**İşlem Adımları:**
1. Standart middleware
2. İçerik filtresi çalıştır (başlık + içerik)
3. Risk seviyesine göre karar:
   - `critical`: gönderi oluşturmayı reddet, yöneticiye bildirim
   - `high`: gönderi oluşturmayı reddet
   - `medium`: `status = "filtered"`, moderasyon kuyruğuna al
   - `low`: `status = "pending"`, moderasyon kuyruğuna al
4. `posts` koleksiyonuna yaz (author snapshot ile birlikte)
5. `moderationQueue` belgesini oluştur
6. Kullanıcıya bildirim gönder ("Gönderiniz incelemeye alındı")
7. `postId` döndür

---

### CF-POST-002: `moderatePost`

**Tür:** onCall  
**Gereklilik:** Authentication + Moderatör veya Yönetici rolü

**Giriş:**
```typescript
{
  postId: string;
  action: "approve" | "reject" | "request_edit" | "unpublish";
  note?: string;
  rejectionReason?: string;
}
```

**İşlem Adımları:**
1. Standart middleware + rol kontrolü (moderator veya admin)
2. `posts/{postId}` belgesini oku
3. Durum geçişini doğrula (geçerli bir state machine geçişi mi?)
4. `posts/{postId}` güncellle (status, moderatorId, moderatorNote vb.)
5. `moderationQueue/{postId}` güncelle
6. Gönderi sahibine bildirim gönder
7. `moderationActions` koleksiyonuna kayıt ekle
8. `auditLogs` koleksiyonuna kayıt ekle

---

## 5. Etkileşim Fonksiyonları

### CF-REACT-001: `toggleReaction`

**Tür:** onCall  
**Gereklilik:** Authentication + engel kontrolü

**Giriş:** `{ postId: string; reactionType: "like" | "dislike" }`

**İşlem Adımları (Firestore Transaction):**
1. `postReactions/{postId}_{userId}` belgesini oku
2. Mevcut reaksiyonu belirle (yok / aynı / farklı)
3. `postReactions` güncelle (set veya delete)
4. `posts/{postId}` güncelle:
   - Yok → likeCount++ veya dislikeCount++
   - Aynı → kaldır: likeCount-- veya dislikeCount--
   - Farklı → biri++ diğeri--
5. Transaction commit

**İdempotency:** Aynı `{postId}_{userId}` anahtarı nedeniyle doğal idempotency sağlanır.

---

### CF-REACT-002: `ratePost`

**Tür:** onCall  
**Gereklilik:** Authentication + engel kontrolü

**Giriş:** `{ postId: string; rating: number }` (rating: 1–5)

**İşlem Adımları (Firestore Transaction):**
1. `postRatings/{postId}_{userId}` belgesini oku
2. Mevcut puan varsa: eski puanı çıkar, yenisini ekle
3. Yoksa: yeni puan ekle, ratingCount++
4. `posts/{postId}` → `averageRating` ve `ratingCount` güncelle
5. Transaction commit

---

## 6. Yorum Fonksiyonları

### CF-COMMENT-001: `addComment`

**Tür:** onCall  
**Gereklilik:** Authentication + engel kontrolü

**Giriş:**
```typescript
{
  postId: string;
  parentCommentId?: string;   // null ise yorum, dolu ise cevap
  content: string;
}
```

**İşlem Adımları:**
1. Gönderi `commentsAllowed == true` doğrula
2. Gönderi `status == "approved"` doğrula
3. İçerik filtresini çalıştır
4. Risk yüksekse moderasyon kuyruğuna gönder (`status: "hidden"`)
5. `comments` koleksiyonuna yaz
6. Transaction: `posts/{postId}.commentCount++`
7. Cevapsa: `comments/{parentCommentId}.replyCount++`
8. Gönderi sahibine bildirim gönder

---

## 7. Referans Fonksiyonları

### CF-REF-001: `initiateReference`

**Tür:** onCall  
**Gereklilik:** Authentication + engel kontrolü + kiracı veya ev sahibi rolü

**Giriş:** `{ targetUserId: string; relationshipType: string }`

**İşlem Adımları:**
1. Aynı referans zaten mevcut mu? (authorId + targetId + aktif durum)
2. `references` koleksiyonuna `status: "invite_sent"` ile yaz
3. `referenceRelations` belgesini oluştur
4. Hedef kullanıcıya bildirim gönder ("Bu kişiyi tanıyor musunuz?")

---

### CF-REF-002: `respondToReferenceInvite`

**Tür:** onCall  
**Gereklilik:** Authentication + davet alıcısı olma kontrolü

**Giriş:** `{ referenceId: string; response: "yes" | "no" }`

**İşlem Adımları:**
1. `referenceRelations/{referenceId}` belgesi doğrula (`targetId == callerUid`)
2. Zaten cevap verilmiş mi? → Hata
3. `referenceRelations/{referenceId}` güncelle (`response`, `respondedAt`)
4. "no" ise: `references/{referenceId}` → `status: "not_recognized"` → süreci sonlandır
5. "yes" ise: `references/{referenceId}` → `status: "preparing"` → referans yazma ekranı açılır
6. Talebi başlatana bildirim gönder

---

### CF-REF-003: `submitReference`

**Tür:** onCall  
**Gereklilik:** Authentication + referansı yazan kişi olma kontrolü

**Giriş:** `{ referenceId: string; scores: map; overallRating: number }`

**İşlem Adımları:**
1. `references/{referenceId}` doğrula (`authorId == callerUid`, `status == "preparing"`)
2. `scores` verisini doğrula (tüm alanlar, puan aralıkları)
3. `references/{referenceId}` güncelle (`scores`, `overallRating`, `status: "pending_approval"`)
4. Moderasyon kuyruğuna al
5. Hedef kullanıcıya bildirim gönder ("Hakkınızda referans incelemeye alındı")

---

## 8. Sözleşme Fonksiyonları

### CF-CONTRACT-001: `createContract`

**Tür:** onCall  
**Gereklilik:** Authentication + Kiracı veya Ev Sahibi rolü

**İşlem:** Taslak sözleşme oluşturur, davet göndermez.

---

### CF-CONTRACT-002: `lockContractVersion`

**Tür:** onCall  
**Gereklilik:** Authentication + sözleşmenin tarafı olma kontrolü

**Giriş:** `{ contractId: string }`

**İşlem Adımları:**
1. Her iki tarafın `approved == true` olduğunu doğrula
2. Sözleşme zaten kilitli mi? → Hata
3. Firestore transaction:
   - `contracts/{contractId}` → `isLocked: true`, `lockedAt`, `status: "locked"`
   - `contractVersions` → yeni versiyon kaydı oluştur
4. `generateContractPdf` fonksiyonunu tetikle

---

### CF-CONTRACT-003: `generateContractPdf`

**Tür:** onCall veya iç trigger  
**Gereklilik:** Sistem tarafından tetiklenir (sözleşme kilitlenmesi sonrası)

**İşlem Adımları:**
1. Sözleşme verilerini oku
2. Kilitlenme kontrolü (`isLocked == true`)
3. PDF oluştur (pdfkit veya benzeri kütüphane):
   - Başlık, taraf bilgileri, maddeler, demirbaş listesi
   - Benzersiz belge numarası: `MLKR-{YYYY}-{seq}`
   - Doğrulama kodu: crypto.randomBytes(6).toString('hex').toUpperCase()
   - Bütünlük hash: SHA-256(contractId + version + lockedAt)
4. Firebase Storage'a yükle: `contracts/{contractId}/v{version}.pdf`
5. Storage URL'yi `contracts/{contractId}` belgesine yaz
6. Her iki tarafa bildirim gönder ("PDF hazırlandı")

---

## 9. Satın Alma Fonksiyonları

### CF-BILLING-001: `verifyPurchase`

**Tür:** onCall  
**Gereklilik:** Authentication

**Giriş:** `{ purchaseToken: string; productId: string }`

**İşlem Adımları:**
1. Authentication doğrulama
2. `purchaseToken` Google Play Developer API üzerinden doğrula:
   ```
   GET https://androidpublisher.googleapis.com/androidpublisher/v3/
       applications/{packageName}/purchases/products/{productId}/tokens/{token}
   ```
3. `purchaseState == 0` (Purchased) doğrula
4. Token daha önce kullanıldı mı? (replay attack koruması)
5. `purchases/{userId}` belgesini oluştur/güncelle
6. `users/{userId}.adsRemoved = true` güncelle
7. Başarı döndür

**İdempotency:** Aynı token ikinci kez gönderilirse aynı yanıt döner, işlem tekrar yapılmaz.

---

### CF-BILLING-002: `handleRefund`

**Tür:** onRequest (Google Play RTDN webhook)  
**Gereklilik:** Google Play RTDN imzası doğrulaması

**İşlem:** İade durumunda `purchases/{userId}.isActive = false` ve `users/{userId}.adsRemoved = false` günceller.

---

## 10. Yönetici Fonksiyonları

### CF-ADMIN-001: `setUserRole`

**Tür:** onCall  
**Gereklilik:** Authentication + Admin rolü

**Giriş:** `{ targetUserId: string; role: "moderator" | "tenant" | "landlord" | "both" }`

**İşlem Adımları:**
1. Admin rolü doğrulama
2. Hedef kullanıcı var mı?
3. Firebase Auth custom claims güncelle (Admin SDK)
4. `users/{targetUserId}.role` güncelle
5. `auditLogs` kaydı oluştur

---

### CF-ADMIN-002: `banUser`

**Tür:** onCall  
**Gereklilik:** Authentication + Admin rolü

**Giriş:** `{ targetUserId: string; banType: "temporary" | "permanent"; duration?: number; reason: string }`

**İşlem Adımları:**
1. Admin rolü doğrulama
2. Kendi hesabını engellemeye çalışıyor mu? → Hata
3. `users/{targetUserId}` güncelle (`isBanned`, `banType`, `banExpiresAt`)
4. Firebase Auth custom claims güncelle (`isBanned: true`)
5. Kullanıcının aktif FCM tokenını geçersiz kıl
6. `auditLogs` kaydı oluştur

---

## 11. Scheduled (Zamanlanmış) Fonksiyonlar

### CF-SCHED-001: `cleanupNotifications`

**Zamanlama:** Her gün 02:00 (Europe/Istanbul)  
**İşlem:** 90 günden eski bildirimleri sil.

---

### CF-SCHED-002: `cleanupModerationQueue`

**Zamanlama:** Her gün 03:00  
**İşlem:** İşlemi tamamlanmış (approved/rejected) ve 30 günden eski moderasyon kuyruğu kayıtlarını temizle.

---

### CF-SCHED-003: `checkBanExpiry`

**Zamanlama:** Her saat  
**İşlem:** `banType == "temporary"` ve `banExpiresAt < now()` olan kullanıcıların engelini kaldır.

---

## 12. Firestore Trigger Fonksiyonları

### CF-TRIGGER-001: `onPostStatusChange`

**Tetikleyici:** `posts/{postId}` belgesinin `status` alanı değiştiğinde  
**İşlem:** Gönderi sahibine ilgili bildirimi gönder.

---

### CF-TRIGGER-002: `onReferenceStatusChange`

**Tetikleyici:** `references/{referenceId}` belgesinin `status` alanı değiştiğinde  
**İşlem:** Referans sahibi veya hedef kullanıcıya ilgili bildirimi gönder.

---

## 13. Rate Limiting Kuralları

| Fonksiyon | Limit | Pencere |
|---|---|---|
| submitPost | 10 gönderi | 24 saat |
| addComment | 50 yorum | 1 saat |
| toggleReaction | 100 işlem | 1 saat |
| ratePost | 30 işlem | 1 saat |
| createReport | 20 şikâyet | 24 saat |
| initiateReference | 5 talep | 24 saat |
| createContract | 10 sözleşme | 24 saat |
| verifyPurchase | 3 deneme | 1 saat |

Rate limiting Firestore'da `rateLimits/{userId}/{functionName}` belgesiyle uygulanır.

---

## 14. Hata Kodları

| Kod | HTTP Karşılığı | Açıklama |
|---|---|---|
| `unauthenticated` | 401 | Giriş yapılmamış |
| `permission-denied` | 403 | Yetki yok |
| `not-found` | 404 | Belge bulunamadı |
| `already-exists` | 409 | Benzersizlik ihlali |
| `resource-exhausted` | 429 | Rate limit aşıldı |
| `invalid-argument` | 400 | Geçersiz giriş |
| `failed-precondition` | 412 | İş kuralı ihlali |
| `internal` | 500 | Sunucu hatası |

---

*Sonraki doküman: 15-security-rules-approach.md*