plugins {
    alias(libs.plugins.mulkora.android.feature)
}

android {
    namespace = "com.mulkora.feature.feed"
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":feature:feature-reactions"))
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.coil.compose)
    implementation(libs.play.services.ads)

    // Test bağımlılıkları
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(project(":core:core-testing"))
}
