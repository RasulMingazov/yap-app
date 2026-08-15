plugins {
    alias(libs.plugins.yap.server.application)
}

dependencies {
    implementation(project(":services:server:core-config"))
    implementation(project(":services:server:core-database"))
    implementation(project(":services:server:core-security"))
    implementation(project(":services:server:feature-auth"))
    implementation(project(":shared:contract:common"))
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.slf4j.api)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(project(":shared:contract:auth"))
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.serialization.json)
    testImplementation(libs.ktor.server.test.host)
    // The wiring guard runs the real graph, and the real graph reaches a real database.
    testImplementation(libs.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}
