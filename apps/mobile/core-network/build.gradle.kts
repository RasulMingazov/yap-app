plugins {
    alias(libs.plugins.yap.kmp.library)
    alias(libs.plugins.yap.koin)
    alias(libs.plugins.yap.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.client.core)

            implementation(project(":apps:mobile:core-common"))
            implementation(project(":shared:contract:common"))
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(project(":shared:contract:common"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.serialization.json)
        }
    }
}
