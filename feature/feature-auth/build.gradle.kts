plugins {
    alias(libs.plugins.mulkora.android.feature)
}

android {
    namespace = "com.mulkora.feature.auth"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-firebase"))
    implementation(libs.play.services.auth)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.androidx.datastore.preferences)

    // WorkManager — SyncFcmTokenUseCase için (CLAUDE.md §16: FCM token senkronizasyonu)
    // core-firebase convention plugin tarafından otomatik eklenir; ek bağımlılık gerekmez.
    implementation(libs.androidx.work.runtime.ktx)

    // DataStore erişimi için — SplashViewModel onboarding durumunu buradan okur
    implementation(project(":core:core-database"))

    // Test bağımlılıkları
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(project(":core:core-testing"))
}
