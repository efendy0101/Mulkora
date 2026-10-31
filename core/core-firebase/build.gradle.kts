plugins {
    alias(libs.plugins.mulkora.android.library)
    alias(libs.plugins.mulkora.hilt)
}

android {
    namespace = "com.mulkora.core.firebase"
}

dependencies {
    api(platform(libs.firebase.bom))
    api(libs.firebase.auth.ktx)
    api(libs.firebase.firestore.ktx)
    api(libs.firebase.storage.ktx)
    api(libs.firebase.functions.ktx)
    api(libs.firebase.messaging.ktx)
    api(libs.firebase.config.ktx)

    implementation(project(":core:core-common"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.gson)

    // WorkManager — FcmTokenSyncWorker için (CLAUDE.md §16: FCM token senkronizasyonu)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
}
