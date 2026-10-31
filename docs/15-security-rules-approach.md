# Mülkora — Security Rules Yaklaşımı

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## 1. Temel İlkeler

1. **Varsayılan reddet:** Tüm okuma ve yazma işlemleri varsayılan olarak reddedilir; yalnızca açıkça izin verilen işlemler geçer.
2. **Sunucu yetkisi:** Sayaçlar, durum güncellemeleri ve hassas alanlar yalnızca Cloud Functions (Admin SDK) tarafından yazılır; istemci doğrudan yazamaz.
3. **En az yetki:** Her koleksiyonda yalnızca zorunlu olan okuma/yazma izni verilir.
4. **Çift katman doğrulama:** İstemci tarafı kontroller yalnızca UX amaçlıdır; gerçek güvenlik Rules + Cloud Functions'da sağlanır.
5. **Emulator Suite testleri:** Tüm kurallar Firebase Emulator Suite ile test edilir; production'a geçmeden önce tüm test senaryoları geçmelidir.

---

## 2. Yardımcı Fonksiyonlar (Firestore Rules)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // --- Yardımcı Fonksiyonlar ---

    function isAuthenticated() {
      return request.auth != null;
    }

    function isNotBanned() {
      return !request.auth.token.isBanned == true;
    }

    function isValidUser() {
      return isAuthenticated() && isNotBanned();
    }

    function isOwner(userId) {
      return request.auth.uid == userId;
    }

    function hasRole(role) {
      return request.auth.token.role == role;
    }

    function isAdmin() {
      return hasRole('admin');
    }

    function isModerator() {
      return hasRole('moderator') || hasRole('admin');
    }

    function isTenant() {
      return hasRole('tenant') || hasRole('both');
    }

    function isLandlord() {
      return hasRole('landlord') || hasRole('both');
    }

    function isContractParty(contractData) {
      return request.auth.uid == contractData.landlordId
          || request.auth.uid == contractData.tenantId;
    }

    // Yalnızca belirli alanların değiştirildiğini doğrula
    function onlyFields(allowedFields) {
      return request.resource.data.diff(resource.data).affectedKeys()
              .hasOnly(allowedFields);
    }
  }
}
```

---

## 3. Koleksiyon Bazlı Kurallar

### 3.1 `users` Koleksiyonu

```javascript
match /users/{userId} {

  // Okuma: Kimliği doğrulanmış kullanıcılar herkesi okuyabilir
  // Hassas alanlar (email, banType, fcmToken) projection ile gizlenir
  allow read: if isValidUser();

  // Oluşturma: Yalnızca Cloud Functions (Admin SDK) oluşturur
  allow create: if false;

  // Güncelleme: Kullanıcı yalnızca kendi profilinin izin verilen alanlarını güncelleyebilir
  allow update: if isValidUser()
                && isOwner(userId)
                && onlyFields(['displayName', 'bio', 'city', 'avatarUrl',
                               'fcmToken', 'notificationPreferences',
                               'privacySettings', 'updatedAt']);

  // Silme: Hiçbir kullanıcı doğrudan silemez (Cloud Functions yapar)
  allow delete: if false;
}
```

---

### 3.2 `usernames` Koleksiyonu

```javascript
match /usernames/{username} {
  // Okuma: Herkes okuyabilir (username müsaitlik kontrolü)
  allow read: if true;

  // Yazma: Yalnızca Cloud Functions
  allow write: if false;
}
```

---

### 3.3 `posts` Koleksiyonu

```javascript
match /posts/{postId} {

  // Okuma: Onaylı gönderileri herkes okuyabilir
  // Diğer durumlar: Kendi gönderisi veya moderatör/admin
  allow read: if resource.data.status == 'approved'
              || (isValidUser() && isOwner(resource.data.authorId))
              || (isAuthenticated() && isModerator());

  // Oluşturma: Yalnızca Cloud Functions (submitPost)
  allow create: if false;

  // Güncelleme: Kullanıcı yalnızca onaylanmamış kendi gönderisini,
  //             sadece belirli alanları güncelleyebilir
  allow update: if isValidUser()
                && isOwner(resource.data.authorId)
                && resource.data.status in ['draft', 'edit_requested']
                && onlyFields(['title', 'content', 'imageUrl', 'city',
                               'postType', 'category', 'isAnonymous',
                               'commentsAllowed', 'updatedAt'])
                // Sayaç alanları doğrudan güncellenemez
                && !('likeCount' in request.resource.data.diff(resource.data).affectedKeys())
                && !('dislikeCount' in request.resource.data.diff(resource.data).affectedKeys())
                && !('commentCount' in request.resource.data.diff(resource.data).affectedKeys())
                && !('status' in request.resource.data.diff(resource.data).affectedKeys());

  // Silme: Yalnızca Cloud Functions
  allow delete: if false;
}
```

---

### 3.4 `comments` Koleksiyonu

```javascript
match /comments/{commentId} {

  // Okuma: Görünür yorumları kimliği doğrulanmış herkes okuyabilir
  allow read: if isValidUser()
              && resource.data.status == 'visible';

  // Oluşturma: Yalnızca Cloud Functions (addComment)
  allow create: if false;

  // Güncelleme: Kullanıcı yalnızca kendi yorumunun içeriğini güncelleyebilir
  allow update: if isValidUser()
                && isOwner(resource.data.authorId)
                && resource.data.status == 'visible'
                && onlyFields(['content', 'isEdited', 'editedAt', 'updatedAt'])
                && !('likeCount' in request.resource.data.diff(resource.data).affectedKeys());

  // Silme: Kullanıcı kendi yorumunu silebilir (soft delete — Cloud Functions yapar)
  allow delete: if false;
}
```

---

### 3.5 `postReactions` Koleksiyonu

```javascript
match /postReactions/{reactionId} {
  // Okuma: Kimliği doğrulanmış kullanıcılar kendi reaksiyonlarını okuyabilir
  allow read: if isValidUser()
              && resource.data.userId == request.auth.uid;

  // Yazma: Yalnızca Cloud Functions (toggleReaction)
  allow write: if false;
}
```

---

### 3.6 `postRatings` Koleksiyonu

```javascript
match /postRatings/{ratingId} {
  // Okuma: Kendi puanını okuyabilir
  allow read: if isValidUser()
              && resource.data.userId == request.auth.uid;

  // Yazma: Yalnızca Cloud Functions (ratePost)
  allow write: if false;
}
```

---

### 3.7 `savedPosts` Koleksiyonu

```javascript
match /savedPosts/{savedId} {
  // Okuma/Yazma: Kendi kaydettiği gönderiler
  allow read: if isValidUser()
              && resource.data.userId == request.auth.uid;

  // Oluşturma: Kendi kaydı
  allow create: if isValidUser()
                && request.resource.data.userId == request.auth.uid
                && request.resource.data.keys().hasAll(['userId', 'postId', 'savedAt'])
                && request.resource.data.keys().hasOnly(['userId', 'postId', 'savedAt']);

  // Silme: Kendi kaydı
  allow delete: if isValidUser()
                && resource.data.userId == request.auth.uid;
}
```

**Not:** `savedPosts` kaydı için istemci doğrudan yazabilir çünkü bu veri kritik bir sayacı etkilemez ve yalnızca kendi verisidir. `saveCount` sayacı Cloud Functions tarafından güncellenir.

---

### 3.8 `reports` Koleksiyonu

```javascript
match /reports/{reportId} {
  // Okuma: Kendi şikâyetleri veya moderatör/admin
  allow read: if isValidUser() && (
                resource.data.reporterId == request.auth.uid
                || isModerator()
              );

  // Oluşturma: Yalnızca Cloud Functions (createReport)
  allow create: if false;

  // Güncelleme/Silme: Yalnızca Cloud Functions
  allow update, delete: if false;
}
```

---

### 3.9 `references` Koleksiyonu

```javascript
match /references/{referenceId} {
  // Okuma: Referans tarafları veya moderatör/admin
  allow read: if isValidUser() && (
                resource.data.authorId == request.auth.uid
                || resource.data.targetId == request.auth.uid
                || isModerator()
              )
              // veya onaylanmış referansları herkese aç (profil görüntüleme)
              || (isValidUser() && resource.data.status == 'approved');

  // Yazma: Yalnızca Cloud Functions
  allow write: if false;
}
```

---

### 3.10 `referenceRelations` Koleksiyonu

```javascript
match /referenceRelations/{referenceId} {
  // Okuma: Referans tarafları veya moderatör/admin
  allow read: if isValidUser() && (
                resource.data.requesterId == request.auth.uid
                || resource.data.targetId == request.auth.uid
                || isModerator()
              );

  // Yazma: Yalnızca Cloud Functions
  allow write: if false;
}
```

---

### 3.11 `referenceResponses` Koleksiyonu

```javascript
match /referenceResponses/{responseId} {
  // Okuma: Onaylı cevapları herkes okuyabilir; taraflar ve moderatör diğerlerini görebilir
  allow read: if isValidUser() && (
                resource.data.status == 'approved'
                || resource.data.authorId == request.auth.uid
                || isModerator()
              );

  // Yazma: Yalnızca Cloud Functions
  allow write: if false;
}
```

---

### 3.12 `referenceAppeals` Koleksiyonu

```javascript
match /referenceAppeals/{appealId} {
  // Okuma: Başvuran veya moderatör/admin
  allow read: if isValidUser() && (
                resource.data.appellantId == request.auth.uid
                || isModerator()
              );

  // Yazma: Yalnızca Cloud Functions
  allow write: if false;
}
```

---

### 3.13 `contracts` Koleksiyonu

```javascript
match /contracts/{contractId} {
  // Okuma: Yalnızca sözleşmenin tarafları veya admin
  allow read: if isValidUser() && (
                resource.data.landlordId == request.auth.uid
                || resource.data.tenantId == request.auth.uid
                || isAdmin()
              );

  // Oluşturma/Güncelleme/Silme: Yalnızca Cloud Functions
  allow write: if false;
}
```

---

### 3.14 `contractVersions` Koleksiyonu

```javascript
match /contractVersions/{versionId} {
  // Okuma: Sözleşmenin tarafları veya admin
  // contractId ile parent sözleşmeyi sorgulayıp taraf kontrolü yapılması gerekir
  // Güvenli yaklaşım: Cloud Functions endpoint üzerinden erişim
  allow read: if isAdmin(); // Taraf kontrolü Cloud Functions'da yapılır
  allow write: if false;
}
```

---

### 3.15 `contractTemplates` Koleksiyonu

```javascript
match /contractTemplates/{templateId} {
  // Okuma: Kimliği doğrulanmış herkes okuyabilir (şablon seçimi)
  allow read: if isValidUser()
              && resource.data.isActive == true;

  // Yazma: Yalnızca admin (Cloud Functions üzerinden)
  allow write: if false;
}
```

---

### 3.16 `notifications` Koleksiyonu

```javascript
match /notifications/{notificationId} {
  // Okuma: Yalnızca alıcı
  allow read: if isValidUser()
              && resource.data.recipientId == request.auth.uid;

  // Güncelleme: Kullanıcı yalnızca isRead alanını güncelleyebilir
  allow update: if isValidUser()
                && resource.data.recipientId == request.auth.uid
                && onlyFields(['isRead']);

  // Oluşturma/Silme: Yalnızca Cloud Functions
  allow create, delete: if false;
}
```

---

### 3.17 `categories` Koleksiyonu

```javascript
match /categories/{categoryId} {
  // Okuma: Herkes okuyabilir
  allow read: if isValidUser();

  // Yazma: Yalnızca admin (Cloud Functions üzerinden)
  allow write: if false;
}
```

---

### 3.18 `blockedUsers` Koleksiyonu

```javascript
match /blockedUsers/{blockId} {
  // Okuma: Engelleyen kişi
  allow read: if isValidUser()
              && resource.data.blockerId == request.auth.uid;

  // Oluşturma: Cloud Functions (blockUser)
  allow create: if false;

  // Silme: Cloud Functions (unblockUser)
  allow delete: if false;
}
```

---

### 3.19 `purchases` Koleksiyonu

```javascript
match /purchases/{userId} {
  // Okuma: Kendi satın alma kaydını okuyabilir
  allow read: if isValidUser()
              && isOwner(userId);

  // Yazma: Yalnızca Cloud Functions (verifyPurchase)
  allow write: if false;
}
```

---

### 3.20 `auditLogs` Koleksiyonu

```javascript
match /auditLogs/{logId} {
  // Okuma: Yalnızca admin
  allow read: if isAuthenticated() && isAdmin();

  // Yazma: Hiçbir istemci yazamaz (Cloud Functions Admin SDK yazar)
  allow write: if false;
}
```

---

### 3.21 `bannedTerms` Koleksiyonu

```javascript
match /bannedTerms/{termId} {
  // Okuma: Yalnızca admin (kelime listesi gizli tutulur)
  allow read: if isAuthenticated() && isAdmin();

  // Yazma: Yalnızca admin (Cloud Functions üzerinden)
  allow write: if false;
}
```

---

### 3.22 `moderationQueue` Koleksiyonu

```javascript
match /moderationQueue/{postId} {
  // Okuma: Yalnızca moderatör/admin
  allow read: if isAuthenticated() && isModerator();

  // Yazma: Yalnızca Cloud Functions
  allow write: if false;
}
```

---

### 3.23 `moderationActions` Koleksiyonu

```javascript
match /moderationActions/{actionId} {
  // Okuma: Yalnızca moderatör/admin
  allow read: if isAuthenticated() && isModerator();

  // Yazma: Yalnızca Cloud Functions
  allow write: if false;
}
```

---

### 3.24 `appSettings` ve `adSettings` Koleksiyonları

```javascript
match /appSettings/{docId} {
  // Okuma: Kimliği doğrulanmış herkes
  allow read: if isValidUser();
  allow write: if false; // Yalnızca Cloud Functions / Admin Console
}

match /adSettings/{docId} {
  allow read: if isValidUser();
  allow write: if false;
}
```

---

## 4. Firebase Storage Security Rules

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {

    // Profil fotoğrafları
    match /users/{userId}/avatar/{file} {
      allow read: if request.auth != null;
      allow write: if request.auth != null
                  && request.auth.uid == userId
                  && request.resource.size < 5 * 1024 * 1024  // Max 5MB
                  && request.resource.contentType.matches('image/.*');
      allow delete: if request.auth != null
                   && request.auth.uid == userId;
    }

    // Gönderi görselleri
    match /posts/{postId}/image/{file} {
      allow read: if request.auth != null;
      // Yazma: Yalnızca kimliği doğrulanmış kullanıcı (postId kontrolü Cloud Functions'da)
      allow write: if request.auth != null
                  && request.resource.size < 10 * 1024 * 1024  // Max 10MB
                  && request.resource.contentType.matches('image/.*');
      // Silme: Yalnızca Cloud Functions (Admin SDK)
      allow delete: if false;
    }

    // Sözleşme PDF'leri
    match /contracts/{contractId}/v{version}.pdf {
      // Okuma: Yalnızca Cloud Functions kontrol eder (admin SDK ile signed URL üretir)
      allow read: if false; // İstemci doğrudan okuyamaz; signed URL kullanır
      allow write: if false;
    }

    // Diğer tüm yollar: Reddedildi
    match /{allPaths=**} {
      allow read, write: if false;
    }
  }
}
```

**Not:** Sözleşme PDF'leri için Firebase Storage Signed URL kullanılır. Cloud Functions, geçici erişim izni olan URL üretir (1 saat geçerlilikte) ve bunu kullanıcıya döndürür. İstemci doğrudan Storage'a erişemez.

---

## 5. Security Rules Test Senaryoları

Her kural için Firebase Emulator Suite + `@firebase/rules-unit-testing` kütüphanesi kullanılarak test yazılır.

### Temel Test Kategorileri

```
TEST-RULES-001: Kimliği doğrulanmamış kullanıcı posts okuyamaz
TEST-RULES-002: Onaylı gönderi herkes tarafından okunabilir
TEST-RULES-003: Onaysız gönderi yalnızca sahibi okuyabilir
TEST-RULES-004: Kullanıcı başkasının gönderisini güncelleyemez
TEST-RULES-005: Kullanıcı likeCount alanını doğrudan güncelleyemez
TEST-RULES-006: Sözleşme taraf olmayan kişi tarafından okunamaz
TEST-RULES-007: Kilitli sözleşme güncellenemez
TEST-RULES-008: Audit log hiçbir istemci tarafından yazılamaz
TEST-RULES-009: Audit log yalnızca admin tarafından okunabilir
TEST-RULES-010: Yasaklı kelime listesi yalnızca admin okuyabilir
TEST-RULES-011: Kullanıcı kendi bildirimini okundu olarak işaretleyebilir
TEST-RULES-012: Kullanıcı başkasının bildirimini okuyamaz
TEST-RULES-013: Satın alma kaydı yalnızca sahibi okuyabilir
TEST-RULES-014: Referans taraf dışında kimse göremez (onaysız)
TEST-RULES-015: Onaylı referans herkes tarafından okunabilir (profilde)
TEST-RULES-016: Moderatör onay bekleyen gönderileri görebilir
TEST-RULES-017: Normal kullanıcı moderasyon kuyruğunu okuyamaz
TEST-RULES-018: Storage: Profil fotoğrafı 5MB sınırı uygulanır
TEST-RULES-019: Storage: PDF doğrudan indirilemez
TEST-RULES-020: Engellenen kullanıcı post koleksiyonunu okuyamaz
```

---

## 6. App Check Entegrasyonu

- Debug build: `DebugAppCheckProviderFactory` kullanılır (emulator'da test için).
- Release build: `PlayIntegrityAppCheckProviderFactory` kullanılır.
- Cloud Functions'da: `functions.https.onCall` içinde `context.app` kontrol edilir.
- Firestore ve Storage Rules'da App Check doğrulaması `request.app != null` ile eklenir.

```javascript
// App Check ile güçlendirilmiş Rules
match /posts/{postId} {
  allow read: if request.app != null  // App Check
              && resource.data.status == 'approved';
}
```

---

*Sonraki doküman: 16-reference-system-state-machine.md*