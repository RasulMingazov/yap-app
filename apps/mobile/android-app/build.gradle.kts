import java.util.Properties

plugins {
    alias(libs.plugins.yap.android.application)
}

// Build-time configuration comes from `local.properties` at the repository root (never committed;
// `local.properties.example` lists the keys). A Gradle property of the same name overrides it.
// Values are left empty rather than failing here, so builds that never run the app — the server
// image, Detekt — configure without the file; `AppConfiguration` rejects a blank value at startup.
val localProperties: Provider<Properties> = providers
    .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
    .asText
    .map { text -> Properties().apply { load(text.reader()) } }

fun yapProperty(name: String): String = providers.gradleProperty(name)
    .orElse(localProperties.map { properties -> properties.getProperty(name) })
    .getOrElse("")
    .trim()

fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val apiBaseUrl = yapProperty("yap.apiBaseUrl")
val googleWebClientId = yapProperty("yap.google.webClientId")
val googleAndroidClientId = yapProperty("yap.google.androidClientId")

// The browser fallback (research.md R14) returns to the reversed Android client ID scheme. AppAuth's
// `RedirectUriReceiverActivity` declares the manifest placeholder, so it needs a value even when no
// client ID is configured yet.
val googleRedirectScheme = googleAndroidClientId
    .takeIf { it.isNotEmpty() }
    ?.let { "com.googleusercontent.apps." + it.removeSuffix(".apps.googleusercontent.com") }
    ?: "app.yap.oauth"

android {
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "API_BASE_URL", apiBaseUrl.asBuildConfigString())
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", googleWebClientId.asBuildConfigString())
        buildConfigField("String", "GOOGLE_ANDROID_CLIENT_ID", googleAndroidClientId.asBuildConfigString())
        buildConfigField("String", "GOOGLE_REDIRECT_SCHEME", googleRedirectScheme.asBuildConfigString())
        manifestPlaceholders["appAuthRedirectScheme"] = googleRedirectScheme
    }
}

dependencies {
    implementation(project(":apps:mobile:app-root"))
    implementation(project(":apps:mobile:shared-app"))
    implementation(libs.androidx.core.splashscreen)
}
