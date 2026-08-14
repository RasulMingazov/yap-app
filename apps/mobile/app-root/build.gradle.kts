plugins {
    alias(libs.plugins.yap.kmp.library)
    alias(libs.plugins.yap.compose.multiplatform)
    alias(libs.plugins.yap.koin.compose)
    alias(libs.plugins.yap.navigation3)
    alias(libs.plugins.yap.serialization)
}

kotlin {
    // Koin's `verify()` is JVM-only, so the wiring guards run on the Android host compilation.
    android {
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":apps:mobile:feature-auth:api"))
            implementation(project(":apps:mobile:core-common"))
            implementation(project(":apps:mobile:core-design"))
            implementation(project(":apps:mobile:core-network"))
            implementation(project(":apps:mobile:feature-auth:impl"))
            implementation(libs.lifecycle.runtime.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.stubcall)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.koin.test)
        }
    }
}
