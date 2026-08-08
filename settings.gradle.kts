pluginManagement {
    includeBuild("convention-plugins")

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
        maven {
            url = uri("$rootDir/libs")
        }
    }
}

rootProject.name = "yap-app"

include(":shared:contract:auth")

include(":apps:mobile:android-app")
include(":apps:mobile:shared-app")
include(":apps:mobile:app-root")
include(":apps:mobile:feature-auth")
include(":apps:mobile:core-common")
include(":apps:mobile:core-design")
include(":apps:mobile:core-network")
include(":apps:mobile:core-test")

include(":services:server:app")
include(":services:server:feature-auth")
include(":services:server:core-config")
include(":services:server:core-database")
include(":services:server:core-security")
