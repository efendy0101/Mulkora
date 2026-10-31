package com.mulkora.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mulkora.app.MainActivity
import com.mulkora.app.R
import com.mulkora.core.firebase.worker.FcmTokenSyncWorker

/**
 * FCM Servis.
 * CLAUDE.md Bölüm 22 ve docs/19-notification-system.md
 *
 * Bildirim kanalları ve gösterim mantığı burada yönetilir.
 * FCM token yönetimi Cloud Functions üzerinden yapılır.
 */
class MulkoraFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        // Kanal ID'leri Cloud Functions onNotificationCreated.ts getChannelId() ile eşleşmeli
        const val CHANNEL_ID_GENERAL    = "channel_general"
        const val CHANNEL_ID_REFERENCE  = "channel_reference"
        const val CHANNEL_ID_MODERATION = "channel_moderation"
        const val CHANNEL_ID_COMMENTS   = "channel_comments"
        const val CHANNEL_ID_CONTRACTS  = "channel_contracts"

        // Bildirim türleri (FCM data payload'daki "type" alanı)
        const val TYPE_NEW_COMMENT              = "new_comment"
        const val TYPE_NEW_REPLY                = "new_reply"
        const val TYPE_NEW_LIKE                 = "new_like"
        const val TYPE_NEW_REFERENCE            = "reference_request"
        const val TYPE_REFERENCE_ACCEPTED       = "reference_accepted"
        const val TYPE_REFERENCE_REJECTED       = "reference_rejected"
        const val TYPE_REFERENCE_RESPONSE       = "reference_response"
        const val TYPE_REFERENCE_APPEAL         = "reference_appeal"
        const val TYPE_POST_APPROVED            = "post_approved"
        const val TYPE_POST_REJECTED            = "post_rejected"
        const val TYPE_POST_EDIT_REQUESTED      = "post_edit_requested"
        const val TYPE_CONTRACT_INVITED         = "contract_invited"
        const val TYPE_CONTRACT_UPDATED         = "contract_updated"
        const val TYPE_CONTRACT_APPROVED        = "contract_approved"
        const val TYPE_CONTRACT_PDF_READY       = "contract_pdf_ready"
        const val TYPE_SYSTEM                   = "system"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token yenilendiğinde WorkManager ile Firestore'a kaydet.
        // CLAUDE.md §16: FCM token network gerektirir; WorkManager retry mekanizmasıyla güvenli.
        val inputData = Data.Builder()
            .putString(FcmTokenSyncWorker.KEY_TOKEN, token)
            .build()
        val request = OneTimeWorkRequestBuilder<FcmTokenSyncWorker>()
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(request)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Uygulama ön planda ise ve bildirim verisi varsa göster
        val data = remoteMessage.data
        val notificationType = data["type"]
        val title = remoteMessage.notification?.title ?: data["title"] ?: "Mülkora"
        val body = remoteMessage.notification?.body ?: data["body"] ?: ""

        val channelId = when (notificationType) {
            TYPE_NEW_REFERENCE, TYPE_REFERENCE_ACCEPTED, TYPE_REFERENCE_REJECTED,
            TYPE_REFERENCE_RESPONSE, TYPE_REFERENCE_APPEAL ->
                CHANNEL_ID_REFERENCE
            TYPE_POST_APPROVED, TYPE_POST_REJECTED, TYPE_POST_EDIT_REQUESTED ->
                CHANNEL_ID_MODERATION
            TYPE_NEW_COMMENT, TYPE_NEW_REPLY ->
                CHANNEL_ID_COMMENTS
            TYPE_CONTRACT_INVITED, TYPE_CONTRACT_UPDATED,
            TYPE_CONTRACT_APPROVED, TYPE_CONTRACT_PDF_READY ->
                CHANNEL_ID_CONTRACTS
            else ->
                CHANNEL_ID_GENERAL
        }

        sendNotification(title, body, channelId, data)
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        channelId: String,
        data: Map<String, String>
    ) {
        // Deep link ile MainActivity açılır; navigasyon oradan yönetilir
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, pendingIntentFlags
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Kanal oluştur (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels(notificationManager)
        }

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannels(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_GENERAL,
                    "Genel Bildirimler",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Beğeniler ve genel uygulama bildirimleri"
                },
                NotificationChannel(
                    CHANNEL_ID_REFERENCE,
                    "Referans Bildirimleri",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Referans talepleri, onaylar, reddler ve itirazlar"
                },
                NotificationChannel(
                    CHANNEL_ID_MODERATION,
                    "Moderasyon Bildirimleri",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Gönderi onay ve red bildirimleri"
                },
                NotificationChannel(
                    CHANNEL_ID_COMMENTS,
                    "Yorum Bildirimleri",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Yeni yorum ve cevap bildirimleri"
                },
                NotificationChannel(
                    CHANNEL_ID_CONTRACTS,
                    "Sözleşme Bildirimleri",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Sözleşme davetleri, güncellemeler ve PDF hazır bildirimleri"
                }
            )
            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
    }
}