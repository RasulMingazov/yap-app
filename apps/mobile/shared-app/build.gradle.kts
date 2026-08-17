import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/**
 * Platform entry points and the iOS framework — nothing else.
 *
 * This module deliberately has **no `commonMain` sources**. Kotlin/Native exports a framework
 * module's own public declarations into the generated Objective-C header, so common code living
 * here would widen the Swift surface silently; with none, the header can only carry what the
 * framework explicitly exports plus the three iOS entry points below. The composition root lives
 * in `app-root` for that reason.
 */
plugins {
    alias(libs.plugins.yap.kmp.library)
    alias(libs.plugins.yap.compose.multiplatform)
    alias(libs.plugins.yap.koin.compose)
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
            // `api` for what a platform host touches directly: the capability ports the entry
            // points hand back, and Koin's own type.
            api(project(":apps:mobile:core-common"))
            api(libs.koin.core)
            implementation(project(":apps:mobile:app-root"))
        }
    }
}
