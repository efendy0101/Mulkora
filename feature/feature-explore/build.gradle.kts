plugins {
    alias(libs.plugins.mulkora.android.feature)
    alias(libs.plugins.mulkora.android.compose)
}

android {
    namespace = "com.mulkora.feature.explore"
}

dependencies {
    // Keşfet modülü ek bağımlılık gerektirmiyor;
    // Firestore ve Hilt AndroidFeatureConventionPlugin üzerinden sağlanıyor.

    // Test bağımlılıkları
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(project(":core:core-testing"))
}
