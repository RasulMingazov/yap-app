import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.yap.kmp.library)
    alias(libs.plugins.yap.compose.multiplatform)
    alias(libs.plugins.yap.decompose.compose)
}

kotlin {
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "YapShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":apps:mobile:app-root"))
            implementation(project(":apps:mobile:core-design"))
        }
    }
}
