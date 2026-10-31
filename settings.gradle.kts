pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MulkoraApp"

// App
include(":app")

// Core modules
include(":core:core-common")
include(":core:core-ui")
include(":core:core-designsystem")
include(":core:core-network")
include(":core:core-database")
include(":core:core-firebase")
include(":core:core-testing")

// Feature modules
include(":feature:feature-auth")
include(":feature:feature-onboarding")
include(":feature:feature-feed")
include(":feature:feature-explore")
include(":feature:feature-post-create")
include(":feature:feature-post-detail")
include(":feature:feature-comments")
include(":feature:feature-reactions")
include(":feature:feature-reports")
include(":feature:feature-reference")
include(":feature:feature-contracts")
include(":feature:feature-notifications")
include(":feature:feature-profile")
include(":feature:feature-settings")
include(":feature:feature-billing")
include(":feature:feature-ads")