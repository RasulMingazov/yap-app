package app.yap.convention

import app.yap.convention.extensions.commonMainDependencies
import app.yap.convention.extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project

class SerializationPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.plugins.kotlin.serialization.get().pluginId)

        commonMainDependencies {
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
