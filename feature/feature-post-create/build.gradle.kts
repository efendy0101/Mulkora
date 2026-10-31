plugins {
    alias(libs.plugins.mulkora.android.feature)
}

android {
    namespace = "com.mulkora.feature.postcreate"
}

dependencies {
    implementation(libs.coil.compose)
    implementation(libs.androidx.activity.compose)

    // CreatePostViewModel FeedRepository'yi kullanır (gönderi oluşturma Cloud Function).
    // CLAUDE.md §23: feature-post-create, feature-feed domain katmanına bağımlıdır.
    implementation(project(":feature:feature-feed"))

    // CLAUDE.md §16: EXIF temizleme — ExifStripper sınıfı için.
    implementation(project(":core:core-common"))

    // Görsel yükleme: Firebase Storage
    implementation(project(":core:core-firebase"))

    // Test bağımlılıkları
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(project(":core:core-testing"))
}
