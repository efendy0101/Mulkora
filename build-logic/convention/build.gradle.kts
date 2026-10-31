import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "com.mulkora.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.plugins.android.application.get().run { "$pluginId:$pluginId.gradle.plugin:$version" })
    compileOnly(libs.plugins.android.library.get().run { "$pluginId:$pluginId.gradle.plugin:$version" })
    compileOnly(libs.plugins.kotlin.android.get().run { "$pluginId:$pluginId.gradle.plugin:$version" })
    compileOnly(libs.plugins.kotlin.compose.get().run { "$pluginId:$pluginId.gradle.plugin:$version" })
    compileOnly(libs.plugins.ksp.get().run { "$pluginId:$pluginId.gradle.plugin:$version" })
    compileOnly(libs.plugins.hilt.get().run { "$pluginId:$pluginId.gradle.plugin:$version" })
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "mulkora.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "mulkora.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "mulkora.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidCompose") {
            id = "mulkora.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("mulkoraHilt") {
            id = "mulkora.hilt"
            implementationClass = "HiltConventionPlugin"
        }
    }
}