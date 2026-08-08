plugins {
    alias(libs.plugins.yap.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":apps:mobile:core:common"))
            api(libs.stubcall)
            api(libs.kotlinx.coroutines.test)
        }
    }
}
