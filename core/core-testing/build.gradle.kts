plugins {
    alias(libs.plugins.mulkora.android.library)
}

android {
    namespace = "com.mulkora.core.testing"
}

dependencies {
    // Testing araçları — api olarak export edilir (test modülleri bu bağımlılıkları alır)
    api(libs.junit)
    api(libs.mockk)
    api(libs.turbine)
    api(libs.truth)
    api(libs.robolectric)
    api(libs.kotlinx.coroutines.test)
    api(libs.hilt.android.testing)

    implementation(libs.kotlinx.coroutines.android)
    implementation(project(":core:core-common"))
}