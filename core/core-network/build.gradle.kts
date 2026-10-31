plugins {
    alias(libs.plugins.mulkora.android.library)
    alias(libs.plugins.mulkora.hilt)
}

android {
    namespace = "com.mulkora.core.network"
}

dependencies {
    implementation(project(":core:core-common"))

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}