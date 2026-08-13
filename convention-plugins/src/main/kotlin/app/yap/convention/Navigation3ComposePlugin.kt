package app.yap.convention

import app.yap.convention.extensions.commonMainDependencies
import app.yap.convention.extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project

class Navigation3ComposePlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        commonMainDependencies {
            implementation(libs.koin.compose.navigation3)
            implementation(libs.navigation3.runtime)
            implementation(libs.navigation3.ui)
            implementation(libs.lifecycle.viewmodel.navigation3)
        }
    }
}
