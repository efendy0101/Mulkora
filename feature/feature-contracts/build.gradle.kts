plugins {
    alias(libs.plugins.mulkora.android.feature)
}

android {
    namespace = "com.mulkora.feature.contracts"
}

dependencies {
    // PDF indirme için (signed URL'den)
    implementation(libs.coil.compose)

    // ContractRepositoryImpl: FirebaseFirestore + FirebaseFunctions + FirebaseAuth kullanır
    implementation(project(":core:core-firebase"))

    // Test bağımlılıkları
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(project(":core:core-testing"))
}