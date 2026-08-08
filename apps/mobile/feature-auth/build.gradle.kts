plugins {
    alias(libs.plugins.yap.kmp.library)
    alias(libs.plugins.yap.compose.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":apps:mobile:core:design"))
            implementation(project(":apps:mobile:core:network"))
            implementation(project(":apps:mobile:session"))
            implementation(project(":shared:contract:auth"))
        }
    }
}
