plugins {
    alias(libs.plugins.mulkora.android.application)
    alias(libs.plugins.mulkora.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.mulkora.app"

    defaultConfig {
        applicationId = "com.mulkora.app"
        versionCode = 1
        versionName = "1.0.0"

        // AdMob App ID - from local.properties
        manifestPlaceholders["admobAppId"] = project.findProperty("ADMOB_APP_ID")?.toString()
            ?: "ca-app-pub-3940256099942544~3347511713" // Test ID (debug fallback)
    }

    signingConfigs {
        create("release") {
            // Release signing configured via CI or local.properties
            // storeFile = file(project.findProperty("RELEASE_KEYSTORE_PATH").toString())
            // storePassword = project.findProperty("RELEASE_KEYSTORE_PASSWORD").toString()
            // keyAlias = project.findProperty("RELEASE_KEY_ALIAS").toString()
            // keyPassword = project.findProperty("RELEASE_KEY_PASSWORD").toString()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    // Core modules
    implementation(project(":core:core-common"))
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-designsystem"))
    implementation(project(":core:core-firebase"))
    implementation(project(":core:core-database"))

    // Feature modules
    implementation(project(":feature:feature-auth"))
    implementation(project(":feature:feature-onboarding"))
    implementation(project(":feature:feature-feed"))
    implementation(project(":feature:feature-explore"))
    implementation(project(":feature:feature-post-create"))
    implementation(project(":feature:feature-post-detail"))
    implementation(project(":feature:feature-comments"))
    implementation(project(":feature:feature-reactions"))
    implementation(project(":feature:feature-reports"))
    implementation(project(":feature:feature-reference"))
    implementation(project(":feature:feature-contracts"))
    implementation(project(":feature:feature-notifications"))
    implementation(project(":feature:feature-profile"))
    implementation(project(":feature:feature-settings"))
    implementation(project(":feature:feature-billing"))
    implementation(project(":feature:feature-ads"))

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)

    // Firebase BOM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    // AdMob
    implementation(libs.play.services.ads)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}