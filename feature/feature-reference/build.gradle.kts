plugins {
    alias(libs.plugins.mulkora.android.feature)
}

android {
    namespace = "com.mulkora.feature.reference"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":feature:feature-auth"))
    implementation(libs.coil.compose)

    // Test bağımlılıkları
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(project(":core:core-testing"))
}
