package app.yap.convention.extensions

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

internal val Project.libs: LibrariesForLibs
    get() = the<LibrariesForLibs>()

internal val Project.projectJavaVersion: JavaVersion
    get() = JavaVersion.toVersion(libs.versions.java.get())

internal val Project.defaultAndroidNamespace: String
    get() = buildString {
        append("app.yap")
        path
            .removePrefix(":")
            .split(":")
            .flatMap { segment -> segment.split("-") }
            .filter(String::isNotBlank)
            .forEach { segment ->
                append('.')
                append(segment)
            }
    }

internal fun Project.kotlinMultiplatform(
    block: KotlinMultiplatformExtension.() -> Unit,
) {
    extensions.configure(KotlinMultiplatformExtension::class.java, block)
}

internal fun Project.commonMainDependencies(
    block: KotlinDependencyHandler.() -> Unit,
) {
    kotlinMultiplatform {
        sourceSets.commonMain.dependencies(block)
    }
}

internal fun Project.commonTestDependencies(
    block: KotlinDependencyHandler.() -> Unit,
) {
    kotlinMultiplatform {
        sourceSets.commonTest.dependencies(block)
    }
}
