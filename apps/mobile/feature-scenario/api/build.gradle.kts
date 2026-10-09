plugins {
    alias(libs.plugins.yap.kmp.library)
    alias(libs.plugins.yap.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":apps:mobile:core-common"))
            api(libs.kotlinx.coroutines.core)

            api(libs.navigation3.runtime)
        }
    }
}
