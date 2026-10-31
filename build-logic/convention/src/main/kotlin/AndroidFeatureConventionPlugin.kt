import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("mulkora.android.library")
                apply("mulkora.android.compose")
                apply("mulkora.hilt")
            }

            extensions.configure<LibraryExtension> {
                defaultConfig {
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
            }

            dependencies {
                // Feature modules always depend on core modules
                add("implementation", project(":core:core-common"))
                add("implementation", project(":core:core-ui"))
                add("implementation", project(":core:core-firebase"))

                // Standard feature dependencies
                add("implementation", catalogLib("androidx-lifecycle-viewmodel-ktx"))
                add("implementation", catalogLib("androidx-lifecycle-viewmodel-compose"))
                add("implementation", catalogLib("androidx-lifecycle-runtime-compose"))
                add("implementation", catalogLib("androidx-navigation-compose"))
                add("implementation", catalogLib("androidx-hilt-navigation-compose"))

                // Test
                add("testImplementation", project(":core:core-testing"))
            }
        }
    }
}

// Helper extension to safely resolve catalog libraries
private fun Project.catalogLib(alias: String) =
    extensions.getByType(org.gradle.api.artifacts.VersionCatalogsExtension::class.java)
        .named("libs")
        .findLibrary(alias)
        .get()