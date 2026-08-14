plugins {
    alias(libs.plugins.yap.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.lifecycle.viewmodel)

            api(libs.navigation3.runtime)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
