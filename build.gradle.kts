plugins {
    alias(libs.plugins.yap.android.application) apply false
    alias(libs.plugins.yap.compose.multiplatform) apply false
    alias(libs.plugins.yap.detekt) apply false
    alias(libs.plugins.yap.jvm.library) apply false
    alias(libs.plugins.yap.kmp.library) apply false
    alias(libs.plugins.yap.ktor.server) apply false
    alias(libs.plugins.yap.serialization) apply false
    alias(libs.plugins.yap.server.application) apply false
}

subprojects {
    pluginManager.apply("app.yap.detekt")
}
