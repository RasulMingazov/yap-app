plugins {
    alias(libs.plugins.yap.ktor.server)
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
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)

    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.database.postgresql)
    testImplementation(libs.hikaricp)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.server.status.pages)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}
