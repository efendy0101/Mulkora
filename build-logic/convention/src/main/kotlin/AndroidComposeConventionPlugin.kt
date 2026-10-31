import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<LibraryExtension> {
                buildFeatures {
                    compose = true
                }
            }

            dependencies {
                val bom = catalogLib("androidx-compose-bom")
                add("implementation", platform(bom))
                add("androidTestImplementation", platform(bom))

                add("implementation", catalogLib("androidx-compose-ui"))
                add("implementation", catalogLib("androidx-compose-ui-graphics"))
                add("implementation", catalogLib("androidx-compose-ui-tooling-preview"))
                add("implementation", catalogLib("androidx-compose-material3"))
                add("implementation", catalogLib("androidx-compose-material-icons-extended"))

                add("debugImplementation", catalogLib("androidx-compose-ui-tooling"))
                add("debugImplementation", catalogLib("androidx-compose-ui-test-manifest"))

                add("androidTestImplementation", catalogLib("androidx-compose-ui-test-junit4"))
            }
        }
    }
}

private fun Project.catalogLib(alias: String) =
    extensions.getByType(org.gradle.api.artifacts.VersionCatalogsExtension::class.java)
        .named("libs")
        .findLibrary(alias)
        .get()