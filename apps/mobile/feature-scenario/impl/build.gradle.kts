plugins {
    alias(libs.plugins.yap.kmp.library)
    alias(libs.plugins.yap.compose.multiplatform)
    alias(libs.plugins.yap.koin.compose)
    alias(libs.plugins.yap.navigation3)
    alias(libs.plugins.yap.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    android {
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":apps:mobile:feature-scenario:api"))
            implementation(project(":apps:mobile:feature-auth:api"))
            implementation(project(":apps:mobile:core-common"))
            implementation(project(":apps:mobile:core-design"))
            implementation(project(":apps:mobile:core-network"))
            implementation(project(":shared:contract:common"))
            implementation(project(":shared:contract:scenario"))
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(project(":apps:mobile:core-test"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.stubcall)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}
