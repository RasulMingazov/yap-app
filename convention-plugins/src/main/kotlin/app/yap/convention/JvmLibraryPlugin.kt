package app.yap.convention

import app.yap.convention.extensions.libs
import app.yap.convention.extensions.projectJavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class JvmLibraryPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.plugins.kotlin.jvm.get().pluginId)

        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(projectJavaVersion.majorVersion.toInt())
            compilerOptions.jvmTarget.set(JvmTarget.fromTarget(projectJavaVersion.toString()))
        }

        dependencies {
            "testImplementation"(libs.kotlin.test)
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
