plugins {
    alias(libs.plugins.yap.ktor.server)
}

dependencies {
    implementation(project(":services:server:core-database"))
    implementation(project(":services:server:core-security"))
    implementation(project(":shared:contract:common"))
    implementation(project(":shared:contract:scenario"))
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)

    // V2's foreign keys reference 001's `users` table; the auth module on the test classpath
    // lets Flyway apply V1 before V2, mirroring the production migration order.
    testImplementation(project(":services:server:feature-auth"))
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.database.postgresql)
    testImplementation(libs.hikaricp)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.server.status.pages)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}
