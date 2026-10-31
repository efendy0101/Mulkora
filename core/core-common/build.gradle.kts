plugins {
    alias(libs.plugins.mulkora.android.library)
    alias(libs.plugins.mulkora.hilt)
}

android {
    namespace = "com.mulkora.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)

    // EXIF temizleme — GPS ve kişisel metadata kaldırma (CLAUDE.md §16 güvenlik gereği)
    implementation(libs.androidx.exifinterface)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.exifinterface)
}