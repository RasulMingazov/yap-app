plugins {
    alias(libs.plugins.yap.kmp.library)
    alias(libs.plugins.yap.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":apps:mobile:core-common"))
            api(libs.kotlinx.coroutines.core)

            // `Navigation3ComposePlugin` declares navigation3-runtime with `implementation`, so it
            // never arrives transitively; `AuthNavKey : NavKey` needs it on the api surface, and
            // applying the whole `yap.navigation3` plugin would drag the Compose navigation stack
            // into a module that has no Compose.
            api(libs.navigation3.runtime)
        }
    }
}
