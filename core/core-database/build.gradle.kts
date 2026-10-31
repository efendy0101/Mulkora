plugins {
    alias(libs.plugins.mulkora.android.library)
    alias(libs.plugins.mulkora.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.mulkora.core.database"
}

dependencies {
    implementation(project(":core:core-common"))

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore (kullanıcı tercihleri için)
    implementation(libs.androidx.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}