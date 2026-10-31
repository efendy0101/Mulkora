plugins {
    alias(libs.plugins.mulkora.android.library)
    alias(libs.plugins.mulkora.android.compose)
}

android {
    namespace = "com.mulkora.core.ui"
}

dependencies {
    // Shared UI bileşenleri: core-designsystem'e bağımlı
    implementation(project(":core:core-designsystem"))
    implementation(project(":core:core-common"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Coil
    implementation(libs.coil.compose)
}