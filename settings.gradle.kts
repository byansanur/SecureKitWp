pluginManagement {
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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SecureKitWp"
include(":app")
include(":securekit-bom")
include(":securekit-core")
include(":securekit-integrity")
include(":securekit-crypto")
include(":securekit-network")
include(":securekit-biometric")
include(":securekit-database")

