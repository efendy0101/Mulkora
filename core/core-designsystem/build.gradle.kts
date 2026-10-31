plugins {
    alias(libs.plugins.mulkora.android.library)
    alias(libs.plugins.mulkora.android.compose)
}

android {
    namespace = "com.mulkora.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.tooling.preview)
}