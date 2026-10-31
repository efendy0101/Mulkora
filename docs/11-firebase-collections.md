# Mülkora — Firebase Koleksiyonları

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## Genel Notlar

- Tüm timestamp alanları Firestore `Timestamp` türündedir.
- Tüm belge ID'leri Firestore auto-generated ID veya belirli durumlarda özel anahtardır.
- `deletedAt` alanı bulunan koleksiyonlarda soft delete uygulanır.
- Kullanıcı verisi silme/anonimleştirme akışı için her koleksiyonda `userId` indekslenmelidir.

---

## 1. `users`

**Amaç:** Kullanıcı profil bilgileri.  
**Belge ID:** Firebase Auth UID

| Alan | Tür | Açıklama |
|---|---|---|
| uid | string | Firebase Auth UID (belge ID ile aynı) |
| username | string | Benzersiz kullanıcı adı (küçük harf) |
| displayName | string | Görünen ad |
| email | string | E-posta (sadece backend okuyabilir) |
| role | string | "tenant" / "landlord" / "both" |
| city | string | Şehir |
| bio | string | Kısa biyografi |
| avatarUrl | string | Firebase Storage URL |
| isEmailVerified | boolean | E-posta doğrulandı mı |
| isBanned | boolean | Platform engeli |
| banType | string | "temporary" / "permanent" / null |
| banExpiresAt | Timestamp | null |
| fcmToken | string | FCM push token |
| notificationPreferences | map | Bildirim tercihleri |
| privacySettings | map | Profil/referans görünürlüğü |
| adsRemoved | boolean | Reklam kaldırma satın alındı mı |
| postCount | number | Onaylanmış gönderi sayısı (sayaç) |
| referenceReceivedCount | number | Alınan onaylı referans sayısı |
| averageReferenceScore | number | Ortalama referans puanı |
| createdAt | Timestamp | Kayıt tarihi |
| updatedAt | Timestamp | Son güncelleme |
| deletedAt | Timestamp | null (hesap silindiyse dolu) |
| termsAcceptedAt | Timestamp | Kullanım koşulları onay tarihi |
| kvkkAcceptedAt | Timestamp | KVKK onay tarihi |

**Okuma/Yazma Yetkileri:**
- Kullanıcı kendi belgesinin `displayName`, `bio`, `city`, `avatarUrl` alanlarını güncelleyebilir.
- `role`, `isBanned`, `adsRemoved` alanları yalnızca Cloud Functions tarafından güncellenir.
- `email` alanı yalnızca backend (Admin SDK) okuyabilir.

---

## 2. `usernames`

**Amaç:** Kullanıcı adı benzersizlik rezervasyonu.  
**Belge ID:** Kullanıcı adı (küçük harf)

| Alan | Tür | Açıklama |
|---|---|---|
| uid | string | Sahibi olan kullanıcının UID'si |
| reservedAt | Timestamp | Rezervasyon tarihi |

**Okuma/Yazma Yetkileri:**
- Kimse doğrudan yazamaz; yalnızca Cloud Functions (`reserveUsername`) yazar.
- Okunabilir (kullanıcı adı kullanılıyor mu sorgusu için).

---

## 3. `posts`

**Amaç:** Tüm gönderiler (tüm durumlar dahil).  
**Belge ID:** Auto-generated

| Alan | Tür | Açıklama |
|---|---|---|
| postId | string | Belge ID |
| authorId | string | Yazar UID |
| authorUsername | string | Gönderi oluşturma anındaki kullanıcı adı (snapshot) |
| authorDisplayName | string | Snapshot |
| authorAvatarUrl | string | Snapshot |
| authorRole | string | Gönderi oluşturma anındaki rol (snapshot) |
| isAnonymous | boolean | Anonim paylaşım |
| postType | string | "question" / "experience" / "problem" / "advice" / "info" / "discussion" |
| category | string | Kategori ID |
| title | string | Başlık |
| content | string | İçerik |
| imageUrl | string | null |
| city | string | Şehir |
| commentsAllowed | boolean | Yorumlara izin var mı |
| status | string | "draft" / "pending" / "filtered" / "under_review" / "edit_requested" / "approved" / "rejected" / "unpublished" / "deleted" |
| filterRiskLevel | string | "low" / "medium" / "high" / "critical" / null |
| filterDetails | map | Filtre sonuç detayları |
| moderatorId | string | Son işlem yapan moderatör UID |
| moderatorNote | string | Moderasyon notu |
| rejectionReason | string | Red gerekçesi |
| likeCount | number | Beğeni sayısı |
| dislikeCount | number | Eksi oy sayısı |
| commentCount | number | Yorum sayısı |
| ratingCount | number | Puan veren sayısı |
| averageRating | number | Ortalama puan |
| saveCount | number | Kaydetme sayısı |
| reportCount | number | Şikâyet sayısı |
| createdAt | Timestamp | Oluşturma tarihi |
| updatedAt | Timestamp | Son güncelleme |
| approvedAt | Timestamp | Onay tarihi |
| deletedAt | Timestamp | null |

**Okuma/Yazma Yetkileri:**
- `status == "approved"` belgeler herkese okunabilir.
- `status != "approved"` belgeler yalnızca sahibi ve moderatör/yönetici okuyabilir.
- Sayaç alanları (`likeCount`, `commentCount` vb.) doğrudan yazılamaz; yalnızca Cloud Functions transactionı ile güncellenir.

---

## 4. `comments`

**Amaç:** Gönderi yorumları (posts/{postId}/comments alt koleksiyonu veya root koleksiyon).  
**Yapı:** Root koleksiyon önerilir; `postId` alanıyla filtrelenir.  
**Belge ID:** Auto-generated

| Alan | Tür | Açıklama |
|---|---|---|
| commentId | string | Belge ID |
| postId | string | Ait olduğu gönderi ID |
| parentCommentId | string | null (yorum) / commentId (cevap) |
| authorId | string | Yazar UID |
| authorUsername | string | Snapshot |
| authorDisplayName | string | Snapshot |
| authorAvatarUrl | string | Snapshot |
| authorRole | string | Snapshot |
| content | string | Yorum içeriği |
| status | string | "visible" / "hidden" / "deleted" |
| isEdited | boolean | Düzenlendi mi |
| editedAt | Timestamp | null |
| likeCount | number | Yorum beğeni sayısı |
| replyCount | number | Cevap sayısı |
| reportCount | number | Şikâyet sayısı |
| filterRiskLevel | string | null |
| createdAt | Timestamp | |
| deletedAt | Timestamp | null |

---

## 5. `postReactions`

**Amaç:** Gönderi beğeni/eksi oy kayıtları. Bir kullanıcı bir gönderiye yalnızca bir kayıt yazabilir.  
**Belge ID:** `{postId}_{userId}` (bileşik anahtar — idempotency sağlar)

| Alan | Tür | Açıklama |
|---|---|---|
| postId | string | |
| userId | string | |
| reactionType | string | "like" / "dislike" |
| createdAt | Timestamp | |
| updatedAt | Timestamp | |

---

## 6. `postRatings`

**Amaç:** Gönderi puan kayıtları. Bir kullanıcı bir gönderiye yalnızca bir kez puan verebilir.  
**Belge ID:** `{postId}_{userId}`

| Alan | Tür | Açıklama |
|---|---|---|
| postId | string | |
| userId | string | |
| rating | number | 1–5 |
| createdAt | Timestamp | |
| updatedAt | Timestamp | (Puan değiştirme için) |

---

## 7. `commentReactions`

**Amaç:** Yorum beğeni kayıtları.  
**Belge ID:** `{commentId}_{userId}`

| Alan | Tür | Açıklama |
|---|---|---|
| commentId | string | |
| postId | string | (Sorgu optimizasyonu için) |
| userId | string | |
| reactionType | string | "like" |
| createdAt | Timestamp | |

---

## 8. `savedPosts`

**Amaç:** Kullanıcının kaydettiği gönderiler.  
**Belge ID:** `{userId}_{postId}`

| Alan | Tür | Açıklama |
|---|---|---|
| userId | string | |
| postId | string | |
| savedAt | Timestamp | |

---

## 9. `reports`

**Amaç:** Şikâyet kayıtları.  
**Belge ID:** Auto-generated

| Alan | Tür | Açıklama |
|---|---|---|
| reportId | string | |
| reporterId | string | Şikâyet eden UID |
| targetType | string | "post" / "comment" / "user" / "reference" / "referenceResponse" |
| targetId | string | Şikâyet edilen içerik ID |
| reason | string | Şikâyet nedeni kodu |
| description | string | İsteğe bağlı açıklama |
| status | string | "new" / "reviewing" / "awaiting_info" / "actioned" / "rejected" / "closed" |
| reviewedBy | string | Moderatör UID |
| reviewerNote | string | |
| createdAt | Timestamp | |
| updatedAt | Timestamp | |

**Benzersizlik:** Aynı `reporterId` + `targetId` + `targetType` kombinasyonu yalnızca bir kez izin verilir.

---

## 10. `references`

**Amaç:** Referans ana kaydı.  
**Belge ID:** Auto-generated

| Alan | Tür | Açıklama |
|---|---|---|
| referenceId | string | |
| authorId | string | Referansı yazan |
| targetId | string | Referans yazılan kullanıcı |
| relationshipType | string | "previous_tenant" / "current_tenant" / "previous_landlord" / "current_landlord" |
| authorRole | string | Yazan kişinin referans oluşturma anındaki rolü |
| scores | map | Alan adı → {rating: number, sentiment: string, description: string} |
| overallRating | number | Genel ortalama puan |
| status | string | "invite_sent" / "awaiting_confirmation" / "not_recognized" / "preparing" / "pending_approval" / "approved" / "rejected" / "appealed" / "under_review" / "hidden" / "deleted" |
| moderatorId | string | |
| moderatorNote | string | |
| rejectionReason | string | |
| appealId | string | null |
| reportCount | number | |
| createdAt | Timestamp | |
| updatedAt | Timestamp | |
| approvedAt | Timestamp | null |
| deletedAt | Timestamp | null |

---

## 11. `referenceRelations`

**Amaç:** Tanıma talebi — "Bu kişiyi tanıyor musunuz?" durumu.  
**Belge ID:** `{referenceId}`

| Alan | Tür | Açıklama |
|---|---|---|
| referenceId | string | |
| requesterId | string | Talebi başlatan |
| targetId | string | Tanıma cevabı bekleyen |
| response | string | null / "yes" / "no" |
| respondedAt | Timestamp | null |
| createdAt | Timestamp | |

---

## 12. `referenceResponses`

**Amaç:** Referans cevapları.  
**Belge ID:** Auto-generated

| Alan | Tür | Açıklama |
|---|---|---|
| responseId | string | |
| referenceId | string | |
| authorId | string | Cevap yazan (referans sahibi = targetId) |
| content | string | Cevap metni |
| status | string | "pending_approval" / "approved" / "rejected" / "deleted" |
| moderatorId | string | null |
| createdAt | Timestamp | |
| approvedAt | Timestamp | null |

---

## 13. `referenceAppeals`

**Amaç:** Referans itirazları.  
**Belge ID:** Auto-generated

| Alan | Tür | Açıklama |
|---|---|---|
| appealId | string | |
| referenceId | string | |
| appellantId | string | İtiraz eden (targetId) |
| reason | string | İtiraz gerekçesi |
| status | string | "new" / "reviewing" / "upheld" / "rejected" / "closed" |
| reviewedBy | string | null |
| reviewerDecision | string | null |
| createdAt | Timestamp | |
| updatedAt | Timestamp | |

---

## 14. `contracts`

**Amaç:** Kira sözleşmeleri.  
**Belge ID:** Auto-generated

| Alan | Tür | Açıklama |
|---|---|---|
| contractId | string | |
| landlordId | string | Ev sahibi UID |
| tenantId | string | Kiracı UID |
| templateId | string | Kullanılan şablon |
| status | string | "draft" / "sent" / "under_review" / "edit_requested" / "approved" / "locked" / "pdf_ready" |
| isLocked | boolean | Kilitli mi (değiştirilemez) |
| lockedAt | Timestamp | null |
| landlordApproved | boolean | |
| tenantApproved | boolean | |
| landlordApprovedAt | Timestamp | null |
| tenantApprovedAt | Timestamp | null |
| fields | map | Sözleşme form alanları (tüm içerik) |
| currentVersion | number | Sürüm numarası |
| pdfUrl | string | null (PDF hazır olunca dolu) |
| documentNumber | string | Benzersiz belge numarası |
| verificationCode | string | Doğrulama kodu |
| integrityHash | string | PDF bütünlük hash değeri |
| editRequestNote | string | Düzenleme talebi notu |
| createdAt | Timestamp | |
| updatedAt | Timestamp | |

**Okuma/Yazma Yetkileri:**
- Yalnızca `landlordId` veya `tenantId` okuyabilir.
- Kilitlenmiş sözleşme hiç kimse tarafından değiştirilemez.

---

## 15. `contractVersions`

**Amaç:** Sözleşme sürüm geçmişi.  
**Belge ID:** Auto-generated

| Alan | Tür | Açıklama |
|---|---|---|
| versionId | string | |
| contractId | string | |
| versionNumber | number | |
| fields | map | O sürümdeki form verileri |
| changedBy | string | Değişikliği yapan UID |
| createdAt | Timestamp | |

---

## 16. `contractTemplates`

**Amaç:** Sözleşme şablonları (yönetici tarafından yönetilir).  
**Belge ID:** Auto-generated

| Alan | Tür | Açıklama |
|---|---|---|
| templateId | string | |
| name | string | Şablon adı |
| description | string | |
| sections | array | Şablon bölümleri (madde listesi) |
| isDefault | boolean | Varsayılan şablon mu |
| isActive | boolean | Aktif mi |
| createdBy | string | Yönetici UID |
| createdAt | Timestamp | |
| updatedAt | Timestamp | |

---

## 17. `notifications`

**Amaç:** Uygulama içi bildirimler.  
**Belge ID:** Auto-generated

| Alan | Tür | Açıklama |
|---|---|---|
| notificationId | string | |
| recipientId | string | Alıcı UID |
| type | string | Bildirim türü kodu |
| titleKey | string | Yerelleştirme anahtarı |
| bodyKey | string | |
| data | map | İlgili içerik ID'leri |
| isRead | boolean | Okundu mu |
| createdAt | Timestamp | |

---

## 18. `categories`

**Amaç:** Gönderi kategorileri (yönetici yönetir).  
**Belge ID:** Auto-generated veya sabit kod

| Alan | Tür | Açıklama |
|---|---|---|
| categoryId | string | |
| name | string | Kategori adı |
| slug | string | URL-uyumlu kısa ad |
| isActive | boolean | |
| order | number | Sıralama |
| createdAt | Timestamp | |

---

## 19. `blockedUsers`

**Amaç:** Kullanıcı tarafından engelleme kayıtları.  
**Belge ID:** `{blockerId}_{blockedId}`

| Alan | Tür | Açıklama |
|---|---|---|
| blockerId | string | Engelleyen UID |
| blockedId | string | Engellenen UID |
| blockedAt | Timestamp | |

---

## 20. `moderationQueue`

**Amaç:** Moderasyon kuyruğu özet/hızlı erişim koleksiyonu (posts koleksiyonuyla senkron).  
**Belge ID:** postId ile aynı

| Alan | Tür | Açıklama |
|---|---|---|
| postId | string | |
| authorId | string | |
| status | string | |
| filterRiskLevel | string | |
| queuedAt | Timestamp | |
| priority | number | Yüksek risk = yüksek öncelik |

---

## 21. `moderationActions`

**Amaç:** Moderatör eylem kayıtları.  
**Belge ID:** Auto-generated

| Alan | Tür | Açıklama |
|---|---|---|
| actionId | string | |
| moderatorId | string | |
| targetType | string | "post" / "comment" / "reference" / "user" |
| targetId | string | |
| actionType | string | "approve" / "reject" / "hide" / "warn" / "edit_request" |
| previousStatus | string | |
| newStatus | string | |
| note | string | |
| createdAt | Timestamp | |

---

## 22. `bannedTerms`

**Amaç:** Yasaklı kelime ve ifade listesi.  
**Belge ID:** Auto-generated

| Alan | Tür | Açıklama |
|---|---|---|
| termId | string | |
| term | string | Yasaklı kelime veya desen |
| isRegex | boolean | Regex mi |
| riskLevel | string | "low" / "medium" / "high" / "critical" |
| category | string | "profanity" / "personal_info" / "threat" / "hate_speech" / vb. |
| isActive | boolean | |
| addedBy | string | Yönetici UID |
| createdAt | Timestamp | |
| updatedAt | Timestamp | |

---

## 23. `purchases`

**Amaç:** Google Play Billing satın alma kayıtları.  
**Belge ID:** `{userId}` (tek seferlik ürün, kullanıcı başına bir kayıt)

| Alan | Tür | Açıklama |
|---|---|---|
| userId | string | |
| productId | string | "remove_ads_lifetime" |
| purchaseToken | string | Google Play token (hash'li saklama önerilir) |
| purchaseState | string | "purchased" / "refunded" |
| orderId | string | Google Play order ID |
| verifiedAt | Timestamp | Backend doğrulama tarihi |
| isActive | boolean | İade edilmediyse true |
| createdAt | Timestamp | |
| updatedAt | Timestamp | |

---

## 24. `adSettings`

**Amaç:** Reklam yapılandırması (yönetici yönetir, Remote Config ile de senkron).  
**Belge ID:** "global"

| Alan | Tür | Açıklama |
|---|---|---|
| nativeAdFrequency | number | Her kaçıncı gönderide native reklam |
| interstitialCooldownMinutes | number | Interstitial reklam aralığı |
| dailyInterstitialLimit | number | Günlük interstitial sınırı |
| initialPostsWithoutAd | number | İlk kaç gönderide reklam olmaz |
| updatedAt | Timestamp | |

---

## 25. `appSettings`

**Amaç:** Genel uygulama ayarları.  
**Belge ID:** "global"

| Alan | Tür | Açıklama |
|---|---|---|
| maintenanceMode | boolean | Bakım modu |
| minimumAppVersion | string | Minimum desteklenen sürüm |
| forceUpdateRequired | boolean | Zorunlu güncelleme |
| termsVersion | string | Kullanım koşulları sürümü |
| privacyVersion | string | Gizlilik politikası sürümü |
| updatedAt | Timestamp | |

---

## 26. `auditLogs`

**Amaç:** Yönetici ve moderatör işlem kayıtları.  
**Belge ID:** Auto-generated

| Alan | Tür | Açıklama |
|---|---|---|
| logId | string | |
| adminId | string | İşlemi yapan yönetici/moderatör UID |
| actionType | string | İşlem türü kodu |
| targetType | string | Etkilenen varlık türü |
| targetId | string | Etkilenen varlık ID |
| previousState | map | Önceki durum |
| newState | map | Yeni durum |
| description | string | Yönetici açıklaması |
| moderationActionId | string | İlgili moderasyon kaydı |
| createdAt | Timestamp | |

**Okuma/Yazma Yetkileri:**
- Yalnızca Yönetici (admin rolü) okuyabilir.
- Kimse silemez; yalnızca Cloud Functions yazabilir.

---

*Sonraki doküman: 12-firestore-document-schemas.md*