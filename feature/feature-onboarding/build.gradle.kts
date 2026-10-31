plugins {
    alias(libs.plugins.mulkora.android.feature)
    alias(libs.plugins.mulkora.android.compose)
}

android {
    namespace = "com.mulkora.feature.onboarding"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-designsystem"))
    implementation(project(":core:core-database"))

    // Test bağımlılıkları
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(project(":core:core-testing"))
}
