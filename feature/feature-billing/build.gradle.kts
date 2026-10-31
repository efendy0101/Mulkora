plugins {
    alias(libs.plugins.mulkora.android.feature)
    alias(libs.plugins.mulkora.hilt)
}

android {
    namespace = "com.mulkora.feature.billing"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-firebase"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.billing.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.functions.ktx)
    implementation(libs.firebase.auth.ktx)

    // Test bağımlılıkları
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(project(":core:core-testing"))
}