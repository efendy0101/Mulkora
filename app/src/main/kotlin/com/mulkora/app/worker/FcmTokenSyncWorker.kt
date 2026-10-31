package com.mulkora.app.worker

/**
 * Bu dosya artık kullanılmamaktadır.
 * Kanonik implementasyon core-firebase modülünde bulunmaktadır:
 * [com.mulkora.core.firebase.worker.FcmTokenSyncWorker]
 *
 * CLAUDE.md §16: FCM token senkronizasyonu WorkManager üzerinden yapılır.
 * Modül bağımlılık döngüsünü önlemek için Worker core-firebase'e taşınmıştır.
 */
// Geriye dönük uyumluluk için typealias — bu dosyaya artık doğrudan import olmamalı
typealias FcmTokenSyncWorker = com.mulkora.core.firebase.worker.FcmTokenSyncWorker