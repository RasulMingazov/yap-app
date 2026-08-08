plugins {
    alias(libs.plugins.yap.jvm.library)
}

dependencies {
    implementation(project(":services:server:core-config"))
    implementation(libs.auth0.java.jwt)

    testImplementation(libs.stubcall)
}
