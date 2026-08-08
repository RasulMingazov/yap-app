plugins {
    alias(libs.plugins.yap.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:contract:auth"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
