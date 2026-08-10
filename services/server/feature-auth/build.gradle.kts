import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.yap.ktor.server)
}

// Docker-backed PostgreSQL verification lives in its own source set and task. `check` does not
// depend on it, so `./gradlew build` never fails because a container runtime is missing, and the
// suite is run explicitly: `./gradlew :services:server:feature-auth:integrationTest`.
val integrationTest: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[integrationTest.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[integrationTest.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

// The suite verifies the feature's own adapters, which are internal to the module.
extensions.configure<KotlinJvmProjectExtension> {
    val compilations = target.compilations
    compilations.named(integrationTest.name) {
        associateWith(compilations.getByName(SourceSet.MAIN_SOURCE_SET_NAME))
    }
}

dependencies {
    implementation(project(":services:server:core-database"))
    implementation(project(":services:server:core-security"))
    implementation(project(":shared:contract:auth"))
    implementation(libs.auth0.java.jwt)
    implementation(libs.auth0.jwks.rsa)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.stubcall)

    "integrationTestImplementation"(libs.flyway.core)
    "integrationTestImplementation"(libs.flyway.database.postgresql)
    "integrationTestImplementation"(libs.postgresql)
    "integrationTestImplementation"(libs.testcontainers.postgresql)
}

tasks.register<Test>("integrationTest") {
    description = "Runs the PostgreSQL Testcontainers suite for the auth feature."
    group = "verification"

    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    useJUnitPlatform()
    systemProperty("yap.postgres.image", libs.versions.postgresqlImage.get())
    shouldRunAfter(tasks.named("test"))
}
