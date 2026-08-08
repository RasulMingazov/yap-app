import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "app.yap.convention"

dependencies {
    implementation(libs.gradle.plugin.android.tools)
    implementation(libs.gradle.plugin.compose)
    implementation(libs.gradle.plugin.detekt)
    implementation(libs.gradle.plugin.kotlin)
    implementation(libs.gradle.plugin.kotlin.compose)
    implementation(libs.gradle.plugin.kotlin.serialization)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

private val projectJavaVersion = JavaVersion.toVersion(libs.versions.java.get())

java {
    sourceCompatibility = projectJavaVersion
    targetCompatibility = projectJavaVersion
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(projectJavaVersion.toString()))
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "app.yap.android.application"
            implementationClass = "app.yap.convention.AndroidApplicationPlugin"
        }
        register("detekt") {
            id = "app.yap.detekt"
            implementationClass = "app.yap.convention.DetektPlugin"
        }
        register("composeMultiplatform") {
            id = "app.yap.compose.multiplatform"
            implementationClass = "app.yap.convention.ComposeMultiplatformPlugin"
        }
        register("jvmLibrary") {
            id = "app.yap.jvm.library"
            implementationClass = "app.yap.convention.JvmLibraryPlugin"
        }
        register("kmpLibrary") {
            id = "app.yap.kmp.library"
            implementationClass = "app.yap.convention.KmpLibraryPlugin"
        }
        register("ktorServer") {
            id = "app.yap.ktor.server"
            implementationClass = "app.yap.convention.KtorServerPlugin"
        }
        register("serialization") {
            id = "app.yap.serialization"
            implementationClass = "app.yap.convention.SerializationPlugin"
        }
        register("serverApplication") {
            id = "app.yap.server.application"
            implementationClass = "app.yap.convention.ServerApplicationPlugin"
        }
    }
}
