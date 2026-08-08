package app.yap.convention

import app.yap.convention.extensions.commonTestDependencies
import app.yap.convention.extensions.defaultAndroidNamespace
import app.yap.convention.extensions.kotlinMultiplatform
import app.yap.convention.extensions.libs
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

class KmpLibraryPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.plugins.kotlin.multiplatform.get().pluginId)
        pluginManager.apply(libs.plugins.android.kotlin.multiplatform.library.get().pluginId)

        kotlinMultiplatform {
            iosArm64()
            iosSimulatorArm64()

            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                namespace = defaultAndroidNamespace
                compileSdk = libs.versions.compileSdk.get().toInt()
                minSdk = libs.versions.minSdk.get().toInt()

                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }

        commonTestDependencies {
            implementation(libs.kotlin.test)
        }
    }
}
