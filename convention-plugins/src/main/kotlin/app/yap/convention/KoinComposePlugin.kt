package app.yap.convention

import app.yap.convention.extensions.commonMainDependencies
import app.yap.convention.extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project

class KoinComposePlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.plugins.yap.koin.asProvider().get().pluginId)

        commonMainDependencies {
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
    }
}
