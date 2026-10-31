# Mülkora — Firestore Belge Şemaları (Örnek Belgeler)

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

Bu dosya, her koleksiyon için örnek Firestore belgesi içerir. Gerçek veri yapısını kodlamadan önce referans alınmalıdır.

---

## 1. `users/{uid}`

```json
{
  "uid": "abc123",
  "username": "ayse_kiracı",
  "displayName": "Ayşe Kaya",
  "email": "ayse@example.com",
  "role": "tenant",
  "city": "İstanbul",
  "bio": "3 yıllık kiracı deneyimim var.",
  "avatarUrl": "https://storage.googleapis.com/.../users/abc123/avatar.jpg",
  "isEmailVerified": true,
  "isBanned": false,
  "banType": null,
  "banExpiresAt": null,
  "fcmToken": "fcm_token_string_here",
  "notificationPreferences": {
    "postApproved": true,
    "postRejected": true,
    "newComment": true,
    "commentReply": true,
    "postLiked": false,
    "referenceRequest": true,
    "referenceApproved": true,
    "contractInvite": true,
    "adminAnnouncement": true
  },
  "privacySettings": {
    "profileVisibility": "public",
    "referenceVisibility": "public"
  },
  "adsRemoved": false,
  "postCount": 5,
  "referenceReceivedCount": 2,
  "averageReferenceScore": 4.3,
  "createdAt": "2025-01-15T10:00:00Z",
  "updatedAt": "2025-06-20T14:30:00Z",
  "deletedAt": null,
  "termsAcceptedAt": "2025-01-15T10:00:00Z",
  "kvkkAcceptedAt": "2025-01-15T10:00:00Z"
}
```

---

## 2. `usernames/{username}`

```json
{
  "uid": "abc123",
  "reservedAt": "2025-01-15T10:00:00Z"
}
```

Belge ID: `ayse_kiraci` (küçük harf, Türkçe karakter normalize edilmiş)

---

## 3. `posts/{postId}`

```json
{
  "postId": "post_xyz789",
  "authorId": "abc123",
  "authorUsername": "ayse_kiraci",
  "authorDisplayName": "Ayşe Kaya",
  "authorAvatarUrl": "https://storage.googleapis.com/.../users/abc123/avatar.jpg",
  "authorRole": "tenant",
  "isAnonymous": false,
  "postType": "problem",
  "category": "rent_increase",
  "title": "Ev sahibim yasal sınırı aşan kira artışı yapıyor",
  "content": "Merhaba, ev sahibim bu yıl %100 kira artışı talep etti. Yasal sınır ne kadardır?",
  "imageUrl": null,
  "city": "İstanbul",
  "commentsAllowed": true,
  "status": "approved",
  "filterRiskLevel": null,
  "filterDetails": null,
  "moderatorId": "mod_uid_001",
  "moderatorNote": null,
  "rejectionReason": null,
  "likeCount": 42,
  "dislikeCount": 3,
  "commentCount": 15,
  "ratingCount": 8,
  "averageRating": 4.1,
  "saveCount": 12,
  "reportCount": 0,
  "createdAt": "2025-06-01T09:00:00Z",
  "updatedAt": "2025-06-01T10:30:00Z",
  "approvedAt": "2025-06-01T10:30:00Z",
  "deletedAt": null
}
```

---

## 4. `comments/{commentId}`

```json
{
  "commentId": "comment_aaa111",
  "postId": "post_xyz789",
  "parentCommentId": null,
  "authorId": "def456",
  "authorUsername": "mehmet_mal",
  "authorDisplayName": "Mehmet Demir",
  "authorAvatarUrl": "https://storage.googleapis.com/.../users/def456/avatar.jpg",
  "authorRole": "landlord",
  "content": "TÜFE oranına göre yasal sınır uygulanmalı. Bağkur'dan bilgi alabilirsiniz.",
  "status": "visible",
  "isEdited": false,
  "editedAt": null,
  "likeCount": 18,
  "replyCount": 3,
  "reportCount": 0,
  "filterRiskLevel": null,
  "createdAt": "2025-06-01T11:00:00Z",
  "deletedAt": null
}
```

### Cevap örneği (parentCommentId dolu):

```json
{
  "commentId": "comment_bbb222",
  "postId": "post_xyz789",
  "parentCommentId": "comment_aaa111",
  "authorId": "abc123",
  "authorUsername": "ayse_kiraci",
  "authorDisplayName": "Ayşe Kaya",
  "authorAvatarUrl": "https://...",
  "authorRole": "tenant",
  "content": "Teşekkürler, Bağkur'a danışacağım.",
  "status": "visible",
  "isEdited": false,
  "editedAt": null,
  "likeCount": 5,
  "replyCount": 0,
  "reportCount": 0,
  "filterRiskLevel": null,
  "createdAt": "2025-06-01T11:30:00Z",
  "deletedAt": null
}
```

---

## 5. `postReactions/{postId}_{userId}`

```json
{
  "postId": "post_xyz789",
  "userId": "abc123",
  "reactionType": "like",
  "createdAt": "2025-06-01T11:05:00Z",
  "updatedAt": "2025-06-01T11:05:00Z"
}
```

Belge ID: `post_xyz789_abc123`

---

## 6. `postRatings/{postId}_{userId}`

```json
{
  "postId": "post_xyz789",
  "userId": "ghi789",
  "rating": 5,
  "createdAt": "2025-06-02T08:00:00Z",
  "updatedAt": "2025-06-02T08:00:00Z"
}
```

---

## 7. `reports/{reportId}`

```json
{
  "reportId": "report_rrr111",
  "reporterId": "abc123",
  "targetType": "post",
  "targetId": "post_bad001",
  "reason": "harassment",
  "description": "Bu gönderi kişisel bilgilerimi ifşa ediyor.",
  "status": "reviewing",
  "reviewedBy": "mod_uid_001",
  "reviewerNote": null,
  "createdAt": "2025-06-05T14:00:00Z",
  "updatedAt": "2025-06-05T15:00:00Z"
}
```

---

## 8. `references/{referenceId}`

```json
{
  "referenceId": "ref_zzz999",
  "authorId": "abc123",
  "targetId": "def456",
  "relationshipType": "previous_landlord",
  "authorRole": "tenant",
  "scores": {
    "communication": {
      "rating": 4,
      "sentiment": "positive",
      "description": "Her zaman ulaşılabilirdi."
    },
    "maintenance": {
      "rating": 3,
      "sentiment": "neutral",
      "description": "Yavaş ama sonunda ilgilendi."
    },
    "deposit_process": {
      "rating": 5,
      "sentiment": "positive",
      "description": "Tam zamanında iade etti."
    },
    "contract_compliance": {
      "rating": 5,
      "sentiment": "positive",
      "description": "Sözleşme maddelerine tam uyum sağladı."
    },
    "privacy_respect": {
      "rating": 4,
      "sentiment": "positive",
      "description": "Habersiz ziyaret gelmedi."
    },
    "overall": {
      "rating": 4,
      "sentiment": "positive",
      "description": "Güvenilir bir ev sahibi."
    }
  },
  "overallRating": 4.2,
  "status": "approved",
  "moderatorId": "mod_uid_001",
  "moderatorNote": null,
  "rejectionReason": null,
  "appealId": null,
  "reportCount": 0,
  "createdAt": "2025-05-10T09:00:00Z",
  "updatedAt": "2025-05-11T10:00:00Z",
  "approvedAt": "2025-05-11T10:00:00Z",
  "deletedAt": null
}
```

---

## 9. `referenceRelations/{referenceId}`

```json
{
  "referenceId": "ref_zzz999",
  "requesterId": "abc123",
  "targetId": "def456",
  "response": "yes",
  "respondedAt": "2025-05-09T14:00:00Z",
  "createdAt": "2025-05-08T11:00:00Z"
}
```

---

## 10. `referenceResponses/{responseId}`

```json
{
  "responseId": "refres_hhh222",
  "referenceId": "ref_zzz999",
  "authorId": "def456",
  "content": "Ayşe Hanım çok düzenli ve sorunsuz bir kiracıydı. İyi günler dilerim.",
  "status": "approved",
  "moderatorId": "mod_uid_001",
  "createdAt": "2025-05-12T09:00:00Z",
  "approvedAt": "2025-05-12T11:00:00Z"
}
```

---

## 11. `contracts/{contractId}`

```json
{
  "contractId": "contract_ccc333",
  "landlordId": "def456",
  "tenantId": "abc123",
  "templateId": "template_standard_001",
  "status": "locked",
  "isLocked": true,
  "lockedAt": "2025-07-01T12:00:00Z",
  "landlordApproved": true,
  "tenantApproved": true,
  "landlordApprovedAt": "2025-06-30T15:00:00Z",
  "tenantApprovedAt": "2025-07-01T11:00:00Z",
  "fields": {
    "contractDate": "2025-07-01",
    "propertyAddress": "İstanbul, Kadıköy, ...",
    "propertyType": "apartment",
    "landlordName": "Mehmet Demir",
    "landlordIdNumber": "[MASKELENMIŞ]",
    "tenantName": "Ayşe Kaya",
    "tenantIdNumber": "[MASKELENMIŞ]",
    "monthlyRent": 15000,
    "rentCurrency": "TRY",
    "paymentDay": 5,
    "paymentMethod": "bank_transfer",
    "deposit": 30000,
    "startDate": "2025-07-15",
    "endDate": "2026-07-14",
    "aidat": 500,
    "aidatIncluded": false,
    "electricityResponsibility": "tenant",
    "waterResponsibility": "tenant",
    "gasResponsibility": "tenant",
    "internetResponsibility": "tenant",
    "inventory": [
      {"item": "Buzdolabı", "condition": "İyi"},
      {"item": "Çamaşır Makinesi", "condition": "Orta"}
    ],
    "deliveryCondition": "İyi durumda teslim edilmiştir.",
    "specialTerms": "Evcil hayvan beslenemez.",
    "guarantorName": null,
    "additionalNotes": null
  },
  "currentVersion": 3,
  "pdfUrl": "https://storage.googleapis.com/.../contracts/contract_ccc333/v3.pdf",
  "documentNumber": "MLKR-2025-0001",
  "verificationCode": "ABC123DEF456",
  "integrityHash": "sha256_hash_value_here",
  "editRequestNote": null,
  "createdAt": "2025-06-25T10:00:00Z",
  "updatedAt": "2025-07-01T12:00:00Z"
}
```

---

## 12. `notifications/{notificationId}`

```json
{
  "notificationId": "notif_nnn555",
  "recipientId": "abc123",
  "type": "post_approved",
  "titleKey": "notification.post_approved.title",
  "bodyKey": "notification.post_approved.body",
  "data": {
    "postId": "post_xyz789",
    "postTitle": "Ev sahibim yasal sınırı aşan kira artışı yapıyor"
  },
  "isRead": false,
  "createdAt": "2025-06-01T10:30:00Z"
}
```

---

## 13. `categories/{categoryId}`

```json
{
  "categoryId": "rent_increase",
  "name": "Kira Artışı",
  "slug": "kira-artisi",
  "isActive": true,
  "order": 1,
  "createdAt": "2025-01-01T00:00:00Z"
}
```

---

## 14. `purchases/{userId}`

```json
{
  "userId": "abc123",
  "productId": "remove_ads_lifetime",
  "purchaseToken": "hashed_token_value",
  "purchaseState": "purchased",
  "orderId": "GPA.1234-5678-9012-34567",
  "verifiedAt": "2025-03-10T14:00:00Z",
  "isActive": true,
  "createdAt": "2025-03-10T13:55:00Z",
  "updatedAt": "2025-03-10T14:00:00Z"
}
```

---

## 15. `auditLogs/{logId}`

```json
{
  "logId": "audit_lll777",
  "adminId": "admin_uid_001",
  "actionType": "post_approved",
  "targetType": "post",
  "targetId": "post_xyz789",
  "previousState": {
    "status": "pending"
  },
  "newState": {
    "status": "approved"
  },
  "description": "İçerik kurallara uygun bulundu.",
  "moderationActionId": "modaction_mmm444",
  "createdAt": "2025-06-01T10:30:00Z"
}
```

---

## 16. `bannedTerms/{termId}`

```json
{
  "termId": "term_ttt001",
  "term": "TC kimlik",
  "isRegex": true,
  "riskLevel": "high",
  "category": "personal_info",
  "isActive": true,
  "addedBy": "admin_uid_001",
  "createdAt": "2025-01-20T10:00:00Z",
  "updatedAt": "2025-01-20T10:00:00Z"
}
```

---

## 17. `appSettings/global`

```json
{
  "maintenanceMode": false,
  "minimumAppVersion": "1.0.0",
  "forceUpdateRequired": false,
  "termsVersion": "1.0",
  "privacyVersion": "1.0",
  "updatedAt": "2025-07-01T00:00:00Z"
}
```

---

## 18. `adSettings/global`

```json
{
  "nativeAdFrequency": 9,
  "interstitialCooldownMinutes": 10,
  "dailyInterstitialLimit": 5,
  "initialPostsWithoutAd": 4,
  "updatedAt": "2025-07-01T00:00:00Z"
}
```

---

## Veri Saklama ve Silme Politikası

| Koleksiyon | Saklama Süresi | Silme Davranışı |
|---|---|---|
| users | Hesap silinene kadar | Anonimleştirme (kişisel alanlar temizlenir) |
| posts | Süresiz (silindi durumu) | Soft delete, içerik temizlenir |
| comments | Süresiz (silindi durumu) | Soft delete |
| postReactions | Süresiz | Gönderi silinince cascade |
| postRatings | Süresiz | Gönderi silinince cascade |
| references | Süresiz | Soft delete, yazar anonimleşir |
| contracts | Süresiz (hukuki gereklilik) | Kişisel alanlar anonimleşir |
| notifications | 90 gün | Otomatik temizlik (Cloud Functions scheduled job) |
| auditLogs | 2 yıl | Arşivlenir, silinemez |
| moderationQueue | 30 gün (işlem sonrası) | Otomatik temizlik |

---

*Sonraki doküman: 13-firestore-indexes.md*