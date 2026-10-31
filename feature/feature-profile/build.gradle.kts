plugins {
    alias(libs.plugins.mulkora.android.feature)
}

android {
    namespace = "com.mulkora.feature.profile"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-firebase"))
    implementation(libs.coil.compose)
    implementation(libs.androidx.activity.compose)

    // Test bağımlılıkları
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(project(":core:core-testing"))
}
