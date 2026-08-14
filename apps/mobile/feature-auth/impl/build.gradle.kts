import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension

plugins {
    alias(libs.plugins.yap.kmp.library)
    alias(libs.plugins.yap.compose.multiplatform)
    alias(libs.plugins.yap.koin.compose)
    alias(libs.plugins.yap.navigation3)
    alias(libs.plugins.yap.serialization)
}

val APP_AUTH_REDIRECT_SCHEME = "app.yap.oauth"

// AppAuth's own manifest declares the redirect activity with this placeholder, so every module that
// merges it needs a value. The real reversed Google client ID is supplied by `android-app`; the
// library only needs the merge to succeed (research.md R14).
extensions.configure<KotlinMultiplatformAndroidComponentsExtension> {
    onVariants(selector().all()) { variant ->
        variant.manifestPlaceholders.put("appAuthRedirectScheme", APP_AUTH_REDIRECT_SCHEME)
        variant.hostTests.values.forEach { hostTest ->
            hostTest.manifestPlaceholders.put("appAuthRedirectScheme", APP_AUTH_REDIRECT_SCHEME)
        }
        variant.deviceTests.values.forEach { deviceTest ->
            deviceTest.manifestPlaceholders.put("appAuthRedirectScheme", APP_AUTH_REDIRECT_SCHEME)
        }
    }
}

kotlin {
    android {
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":apps:mobile:feature-auth:api"))
            implementation(project(":apps:mobile:core-common"))
            implementation(project(":apps:mobile:core-design"))
            implementation(project(":apps:mobile:core-network"))
            implementation(project(":shared:contract:auth"))
            implementation(libs.compose.resources)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.appauth)
            implementation(libs.googleid)
        }
        commonTest.dependencies {
            implementation(project(":apps:mobile:core-test"))
            implementation(libs.compose.ui.test)
            implementation(libs.koin.test)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.serialization.json)
            implementation(libs.stubcall)
        }
        // `commonTest`'s Compose UI tests execute here, so `./gradlew build` runs them on every
        // change (T012). Robolectric supplies the Android runtime they need on the host JVM.
        getByName("androidHostTest").dependencies {
            implementation(libs.androidx.compose.ui.test.manifest)
            implementation(libs.compose.ui.test.junit4)
            implementation(libs.junit)
            implementation(libs.koin.test)
            implementation(libs.robolectric)
        }
    }
}

compose.resources {
    packageOfResClass = "app.yap.feature.auth.generated.resources"
}
