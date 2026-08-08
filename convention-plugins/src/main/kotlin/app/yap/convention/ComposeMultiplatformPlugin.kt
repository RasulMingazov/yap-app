package app.yap.convention

import app.yap.convention.extensions.commonMainDependencies
import app.yap.convention.extensions.libs
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class ComposeMultiplatformPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.plugins.compose.multiplatform.get().pluginId)
        pluginManager.apply(libs.plugins.kotlin.compose.get().pluginId)

        commonMainDependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }

        extensions.configure(KotlinMultiplatformAndroidComponentsExtension::class.java) {
            finalizeDsl { extension ->
                extension.androidResources.enable = true
            }
        }
    }
}
