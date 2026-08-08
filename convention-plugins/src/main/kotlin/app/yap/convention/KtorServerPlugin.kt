package app.yap.convention

import app.yap.convention.extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class KtorServerPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("app.yap.jvm.library")
        pluginManager.apply(libs.plugins.kotlin.serialization.get().pluginId)

        dependencies {
            "implementation"(libs.ktor.server.core)
            "implementation"(libs.ktor.server.content.negotiation)
            "implementation"(libs.ktor.serialization.json)
        }
    }
}
