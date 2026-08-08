plugins {
    alias(libs.plugins.yap.server.application)
}

dependencies {
    implementation(project(":services:server:core-config"))
    implementation(project(":services:server:feature-auth"))
}
