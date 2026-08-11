plugins {
    alias(libs.plugins.yap.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":apps:mobile:core-common"))
            api(libs.essenty.instance.keeper)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
