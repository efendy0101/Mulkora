plugins {
    alias(libs.plugins.mulkora.android.feature)
    alias(libs.plugins.mulkora.android.compose)
    alias(libs.plugins.mulkora.hilt)
}

android {
    namespace = "com.mulkora.feature.ads"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":feature:feature-billing"))
    implementation(libs.play.services.ads)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    // Test bağımlılıkları
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(project(":core:core-testing"))
}