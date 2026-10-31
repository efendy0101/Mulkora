# Mülkora — Bildirim Sistemi

**Sürüm:** 1.0  
**Tarih:** 2026-07-19  
**Hazırlayan:** Claude Fable — Analiz Aşaması  
**Durum:** Taslak

---

## 1. Bildirim Türleri

| Tür Kodu | Açıklama | Aksiyon |
|---|---|---|
| `post_approved` | Gönderiniz yayımlandı | Gönderi detayına git |
| `post_rejected` | Gönderiniz reddedildi | Gönderi detayına git |
| `post_edit_requested` | Gönderinizde düzenleme gerekiyor | Gönderi düzenlemeye git |
| `post_unpublished` | Gönderiniz yayından kaldırıldı | Gönderi detayına git |
| `new_comment` | Gönderinize yorum yapıldı | Gönderi detayına git (yoruma scroll) |
| `comment_reply` | Yorumunuza cevap verildi | Gönderi detayına git (cevaba scroll) |
| `comment_liked` | Yorumunuz beğenildi | Gönderi detayına git |
| `reference_invite` | Hakkınızda referans yazılmak isteniyor | Tanıma cevabı ekranına git |
| `reference_recognized_yes` | Referans talebi onaylandı | Referans yazma ekranına git |
| `reference_recognized_no` | Talep kişisi sizi tanımadı | Referans listesine git |
| `reference_submitted` | Hakkınızda referans incelemeye alındı | Referans detayına git |
| `reference_approved` | Referansınız yayımlandı | Referans detayına git |
| `reference_rejected` | Referansınız reddedildi | Referans listesine git |
| `reference_appeal_result` | İtirazınız sonuçlandı | Referans detayına git |
| `contract_invite` | Kira sözleşmesi daveti | Sözleşme detayına git |
| `contract_approved` | Sözleşme her iki tarafça onaylandı | Sözleşme detayına git |
| `contract_edit_requested` | Sözleşmede düzenleme talep edildi | Sözleşme detayına git |
| `contract_pdf_ready` | Kira sözleşmesi PDF'i hazırlandı | PDF indirme ekranına git |
| `warning_received` | Moderatörden uyarı aldınız | Bildirim detayı |
| `admin_announcement` | Sistem duyurusu | Bildirim detayı |

---

## 2. Push Bildirim Altyapısı (FCM)

### Token Yönetimi

```kotlin
// FcmTokenManager.kt
class FcmTokenManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun refreshAndSaveToken() {
        val token = FirebaseMessaging.getInstance().token.await()
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("users").document(userId)
            .update("fcmToken", token)
            .await()
    }
    
    // Uygulama başlangıcında ve token yenilendiğinde çağrılır
}
```

### Topic Abonelikleri

```
Tüm kullanıcılar → topic: "all_users"
Admin duyuruları → topic: "announcements"
Şehir bazlı     → topic: "city_{slug}" (ör. "city_istanbul")
```

---

## 3. Cloud Functions Bildirim Gönderimi

```typescript
// functions/src/notifications/sendPushNotification.ts

interface NotificationPayload {
  recipientId: string;
  type: NotificationType;
  data: Record<string, string>;
  titleKey: string;
  bodyKey: string;
  bodyParams?: Record<string, string>;
}

export async function sendNotification(
  payload: NotificationPayload,
  db: FirebaseFirestore.Firestore,
  messaging: FirebaseMessaging.Messaging
): Promise<void> {
  
  // 1. Kullanıcının bildirim tercihlerini kontrol et
  const userDoc = await db.collection('users').doc(payload.recipientId).get();
  const prefs = userDoc.data()?.notificationPreferences ?? {};
  
  if (!isNotificationEnabled(payload.type, prefs)) {
    return; // Bu bildirim türü kullanıcı tarafından kapatılmış
  }
  
  // 2. Firestore'a uygulama içi bildirim yaz
  await db.collection('notifications').add({
    recipientId: payload.recipientId,
    type: payload.type,
    titleKey: payload.titleKey,
    bodyKey: payload.bodyKey,
    data: payload.data,
    isRead: false,
    createdAt: FieldValue.serverTimestamp()
  });
  
  // 3. FCM push bildirim gönder (FCM token varsa)
  const fcmToken = userDoc.data()?.fcmToken;
  if (!fcmToken) return;
  
  try {
    await messaging.send({
      token: fcmToken,
      notification: {
        title: resolveTitle(payload.titleKey, payload.bodyParams),
        body: resolveBody(payload.bodyKey, payload.bodyParams)
      },
      data: {
        type: payload.type,
        ...payload.data
      },
      android: {
        priority: 'high',
        notification: {
          channelId: getChannelId(payload.type),
          sound: 'default'
        }
      }
    });
  } catch (error) {
    if (isInvalidTokenError(error)) {
      // Token geçersiz → Firestore'dan temizle
      await db.collection('users').doc(payload.recipientId)
        .update({ fcmToken: FieldValue.delete() });
    }
  }
}
```

---

## 4. Android Bildirim Kanalları

Android 8.0+ için bildirim kanalları `NotificationChannelManager.kt` içinde tanımlanır:

| Kanal ID | Kanal Adı | Önem Seviyesi |
|---|---|---|
| `channel_post` | Gönderi Bildirimleri | HIGH |
| `channel_comment` | Yorum Bildirimleri | DEFAULT |
| `channel_reference` | Referans Bildirimleri | HIGH |
| `channel_contract` | Sözleşme Bildirimleri | HIGH |
| `channel_moderation` | Moderasyon Bildirimleri | HIGH |
| `channel_announcement` | Duyurular | DEFAULT |
| `channel_general` | Genel | DEFAULT |

---

## 5. Android Bildirim İşleyicisi

```kotlin
// MulkoraFirebaseMessagingService.kt
@AndroidEntryPoint
class MulkoraFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var tokenManager: FcmTokenManager

    override fun onNewToken(token: String) {
        // Token yenilendiğinde Firestore'a güncelle
        CoroutineScope(Dispatchers.IO).launch {
            tokenManager.refreshAndSaveToken()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: return
        val notificationData = message.data
        
        // Ön planda (foreground) bildirim göster
        if (isAppInForeground()) {
            showForegroundNotification(type, notificationData)
        }
        // Arka planda (background) — FCM otomatik gösterir
    }
    
    private fun showForegroundNotification(
        type: String,
        data: Map<String, String>
    ) {
        val pendingIntent = buildDeepLinkIntent(type, data)
        
        NotificationCompat.Builder(this, getChannelId(type))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(data["title"] ?: "")
            .setContentText(data["body"] ?: "")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
            .also { notification ->
                NotificationManagerCompat.from(this)
                    .notify(generateNotifId(data), notification)
            }
    }
}
```

---

## 6. Uygulama İçi Bildirim Merkezi

```kotlin
// NotificationViewModel.kt
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    val notifications: StateFlow<List<Notification>> = repository
        .getNotificationsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val unreadCount: StateFlow<Int> = notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }
}
```

---

## 7. Sistem Duyuruları (Admin Announcement)

Admin, yönetim panelinden tüm kullanıcılara push bildirim gönderebilir.

```typescript
// Cloud Function: sendAdminAnnouncement (admin only)
export async function sendAdminAnnouncement(
  data: { title: string; body: string; targetTopic: string },
  auth: CallableRequest['auth']
): Promise<void> {
  
  // Admin rolü doğrulama
  if (auth?.token.role !== 'admin') {
    throw new HttpsError('permission-denied', 'Admin only');
  }
  
  // FCM topic mesajı gönder
  await getMessaging().send({
    topic: data.targetTopic, // 'all_users' veya 'city_istanbul' vb.
    notification: {
      title: data.title,
      body: data.body
    },
    android: {
      priority: 'high'
    }
  });
  
  // Tüm aktif kullanıcılara Firestore bildirimi de yaz (toplu yazma)
  // Bu işlem ayrı bir batch job olarak yapılır
  await createBulkNotifications(data);
  
  // Audit log
  await createAuditLog({
    adminId: auth.uid,
    actionType: 'admin_announcement_sent',
    description: data.title
  });
}
```

---

## 8. Bildirim Tercihleri

Kullanıcı her bildirim türünü açıp kapatabilir. Tercihler `users/{uid}.notificationPreferences` alanında saklanır.

```kotlin
// NotificationPreferenceKey.kt
enum class NotificationPreferenceKey(val key: String) {
    POST_APPROVED("postApproved"),
    POST_REJECTED("postRejected"),
    NEW_COMMENT("newComment"),
    COMMENT_REPLY("commentReply"),
    POST_LIKED("postLiked"),
    REFERENCE_REQUEST("referenceRequest"),
    REFERENCE_APPROVED("referenceApproved"),
    CONTRACT_INVITE("contractInvite"),
    ADMIN_ANNOUNCEMENT("adminAnnouncement")
}
```

Bir bildirim türü kullanıcı tarafından kapatılırsa Cloud Functions bu kontrolü yapar ve o tür için FCM bildirimi göndermez. Ancak Firestore'daki uygulama içi bildirim kaydı yine de oluşturulabilir (kullanıcı bildirim merkezinden görebilir).

---

## 9. Bildirim Saklanma Politikası

- Firestore'daki bildirimler 90 gün sonra `cleanupNotifications` zamanlanmış fonksiyonu tarafından silinir.
- Android'de yerel olarak gösterilen push bildirimler kullanıcı tarafından kapatılabilir.
- Okunmamış bildirim sayısı alt navigasyon çubuğundaki bildirim sekmesinde rozet olarak gösterilir.

---

## 10. Bildirim Deep Link Yapısı

FCM payload'ındaki `data` alanına göre bildirime tıklandığında ilgili ekrana yönlendirme yapılır.

```kotlin
// NotificationDeepLinkHandler.kt
fun buildDeepLinkIntent(type: String, data: Map<String, String>): PendingIntent {
    val intent = when (type) {
        "post_approved", "post_rejected", "new_comment" ->
            Intent(context, MainActivity::class.java).apply {
                putExtra("deeplink", "mulkora://post/${data["postId"]}")
            }
        "reference_invite", "reference_approved" ->
            Intent(context, MainActivity::class.java).apply {
                putExtra("deeplink", "mulkora://reference/${data["referenceId"]}")
            }
        "contract_invite", "contract_pdf_ready" ->
            Intent(context, MainActivity::class.java).apply {
                putExtra("deeplink", "mulkora://contract/${data["contractId"]}")
            }
        else ->
            Intent(context, MainActivity::class.java).apply {
                putExtra("deeplink", "mulkora://notifications")
            }
    }
    return PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
```

---

*Sonraki doküman: 20-ads-billing-design.md*