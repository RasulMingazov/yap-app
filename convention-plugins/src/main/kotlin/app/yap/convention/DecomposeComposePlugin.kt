package app.yap.convention

import app.yap.convention.extensions.commonMainDependencies
import app.yap.convention.extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project

class DecomposeComposePlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        commonMainDependencies {
            implementation(libs.decompose)
            implementation(libs.decompose.extensions.compose)
            implementation(libs.essenty.lifecycle)
        }
    }
}
