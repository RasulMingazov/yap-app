plugins {
    alias(libs.plugins.yap.ktor.server)
}

dependencies {
    implementation(project(":services:server:core:database"))
    implementation(project(":services:server:core:security"))
    implementation(project(":shared:contract:auth"))
}
