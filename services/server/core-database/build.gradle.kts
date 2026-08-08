plugins {
    alias(libs.plugins.yap.jvm.library)
}

dependencies {
    implementation(project(":services:server:core-config"))
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.postgresql)
    implementation(libs.slf4j.api)
    implementation(libs.kotlinx.coroutines.core)
}
