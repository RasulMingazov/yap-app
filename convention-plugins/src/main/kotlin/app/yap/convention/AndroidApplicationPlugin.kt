package app.yap.convention

import app.yap.convention.extensions.libs
import app.yap.convention.extensions.projectJavaVersion
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidApplicationPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.plugins.android.application.get().pluginId)
        pluginManager.apply(libs.plugins.compose.multiplatform.get().pluginId)
        pluginManager.apply(libs.plugins.kotlin.compose.get().pluginId)

        extensions.configure<ApplicationExtension> {
            namespace = "app.yap"
            compileSdk = libs.versions.compileSdk.get().toInt()

            defaultConfig {
                applicationId = "app.yap"
                minSdk = libs.versions.minSdk.get().toInt()
                targetSdk = libs.versions.targetSdk.get().toInt()
                versionCode = 1
                versionName = "1.0"
            }

            compileOptions {
                sourceCompatibility = projectJavaVersion
                targetCompatibility = projectJavaVersion
            }

            buildFeatures {
                compose = true
            }
        }

        dependencies {
            "implementation"(libs.androidx.activity.compose)
        }
    }
}
