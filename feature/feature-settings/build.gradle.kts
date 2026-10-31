plugins {
    alias(libs.plugins.mulkora.android.feature)
}

android {
    namespace = "com.mulkora.feature.settings"
}

dependencies {
    // SettingsViewModel: AuthRepository, ProfileRepository, BillingRepository inject eder.
    // Bu interface'lere compile-time erişim için ilgili feature modülleri gereklidir.
    implementation(project(":feature:feature-auth"))
    implementation(project(":feature:feature-profile"))
    implementation(project(":feature:feature-billing"))

    // Google Play Billing — BillingRepositoryImpl tarafından kullanılır
    // (feature-billing transitif olarak sağlar, yedek olarak tutulur)
    implementation(libs.billing.ktx)

    // Test bağımlılıkları
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(project(":core:core-testing"))
}
