plugins {
    alias(libs.plugins.yap.kmp.library)
    alias(libs.plugins.yap.compose.multiplatform)
    alias(libs.plugins.yap.koin.compose)
    alias(libs.plugins.yap.navigation3)
    alias(libs.plugins.yap.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":apps:mobile:core-design"))
            implementation(project(":apps:mobile:core-network"))
            implementation(project(":apps:mobile:feature-auth"))
        }
    }
}
