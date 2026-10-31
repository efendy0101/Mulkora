plugins {
    alias(libs.plugins.mulkora.android.feature)
}

android {
    namespace = "com.mulkora.feature.notifications"
}

dependencies {
    implementation(project(":core:core-common"))
    // NotificationSettingsViewModel: FirebaseAuth + FirebaseFirestore kullanır.
    implementation(project(":core:core-firebase"))

    // Test bağımlılıkları
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(project(":core:core-testing"))
}
