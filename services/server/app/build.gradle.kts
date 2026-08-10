plugins {
    alias(libs.plugins.yap.server.application)
}

dependencies {
    implementation(project(":services:server:core-config"))
    implementation(project(":services:server:core-database"))
    implementation(project(":services:server:core-security"))
    implementation(project(":services:server:feature-auth"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.server.status.pages)

    testImplementation(libs.ktor.server.test.host)
}
