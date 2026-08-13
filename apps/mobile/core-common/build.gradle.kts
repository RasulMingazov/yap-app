plugins {
    alias(libs.plugins.yap.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
