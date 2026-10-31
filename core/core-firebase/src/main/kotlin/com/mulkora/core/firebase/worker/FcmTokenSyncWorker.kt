package com.mulkora.core.firebase.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

/**
 * FCM token'ını Firestore'daki kullanıcı belgesine kaydeden Worker.
 *
 * CLAUDE.md §16 — Bildirim sistemi:
 * FCM token yenilendiğinde network gereklidir; WorkManager arka planda
 * retry mekanizmasıyla güvenli biçimde kaydeder.
 *
 * docs/19-notification-system.md: Token, users/{uid} belgesinde
 * "fcmTokens" dizisi alanında saklanır. Çok cihaz desteği için dizi kullanılır.
 * Cloud Functions (onNotificationCreated) bu diziyi okuyarak MulticastMessage gönderir.
 *
 * NOT: Bu worker core-firebase modülünde tanımlanmıştır; hem app.service
 * hem de feature-auth modülleri bağımsız olarak kullanabilir.
 *
 * Input data key: [KEY_TOKEN]
 */
@HiltWorker
class FcmTokenSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_TOKEN = "fcm_token"
        const val WORK_NAME = "fcm_token_sync"
    }

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_TOKEN)
            ?: return Result.failure()

        val uid = auth.currentUser?.uid
            ?: return Result.success() // Oturum açılmamış; token sonraki girişte kaydedilir

        return try {
            // fcmTokens dizisine token ekle (arrayUnion — tekrarlı eklemeyi önler).
            // Cloud Functions onNotificationCreated bu diziyi okuyarak MulticastMessage gönderir.
            firestore
                .collection("users")
                .document(uid)
                .update("fcmTokens", FieldValue.arrayUnion(token))
                .await()
            Result.success()
        } catch (e: Exception) {
            // Retry mekanizması — WorkManager otomatik tekrar dener
            Result.retry()
        }
    }
}