package app.yap.convention

import app.yap.convention.extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaApplication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class ServerApplicationPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("app.yap.ktor.server")
        pluginManager.apply(ApplicationPlugin::class.java)

        extensions.configure<JavaApplication> {
            mainClass.set("app.yap.server.app.ApplicationKt")
        }

        dependencies {
            "implementation"(libs.ktor.server.netty)
        }
    }
}
