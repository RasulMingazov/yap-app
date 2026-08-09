import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test

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
    testImplementation(libs.stubcall)

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
