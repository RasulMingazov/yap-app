plugins {
    alias(libs.plugins.yap.kmp.library)
    alias(libs.plugins.yap.compose.multiplatform)
    alias(libs.plugins.yap.decompose.compose)
    alias(libs.plugins.yap.serialization)
}

kotlin {
    android {
        withHostTestBuilder {}.configure {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":apps:mobile:core-common"))
            implementation(project(":apps:mobile:core-decompose"))
            implementation(project(":apps:mobile:core-design"))
            implementation(project(":apps:mobile:core-network"))
            implementation(project(":shared:contract:auth"))
            implementation(libs.compose.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(project(":apps:mobile:core-test"))
        }
    }
}

compose.resources {
    packageOfResClass = "app.yap.feature.auth.generated.resources"
}
