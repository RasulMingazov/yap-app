plugins {
    alias(libs.plugins.yap.kmp.library)
    alias(libs.plugins.yap.compose.multiplatform)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.compose.resources)
    }
}

compose.resources {
    packageOfResClass = "app.yap.core.design.generated.resources"
}
