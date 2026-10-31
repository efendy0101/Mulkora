package com.mulkora.feature.auth.domain.usecase

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mulkora.core.firebase.worker.FcmTokenSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * FCM token'ını WorkManager aracılığıyla Firestore'a kaydeden use case.
 *
 * CLAUDE.md §16 — Bildirim sistemi:
 * FCM token her oturum açılışında veya token yenilendiğinde senkronize edilmelidir.
 *
 * Worker, core-firebase modülünde tanımlanmıştır ve hem feature-auth hem de app
 * modülleri tarafından erişilebilir. Bağımlılık döngüsü yoktur.
 */
class SyncFcmTokenUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    operator fun invoke(token: String) {
        val inputData = Data.Builder()
            .putString(FcmTokenSyncWorker.KEY_TOKEN, token)
            .build()
        val request = OneTimeWorkRequestBuilder<FcmTokenSyncWorker>()
            .setInputData(inputData)
            .build()
        // REPLACE: Aynı anda birden fazla token sync isteği varsa yenisi eskilerin önüne geçer
        WorkManager.getInstance(context)
            .enqueueUniqueWork(FcmTokenSyncWorker.WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}