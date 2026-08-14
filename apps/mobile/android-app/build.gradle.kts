plugins {
    alias(libs.plugins.yap.android.application)
}

android {
    defaultConfig {
        // AppAuth's `RedirectUriReceiverActivity` declares this placeholder in its own manifest, so
        // it must be defined even before a real client ID is configured. The redirect URI is the
        // reversed Google client ID; supply it as `yap.google.reversedClientId` in
        // `local.properties` or on the command line (research.md R14).
        manifestPlaceholders["appAuthRedirectScheme"] =
            providers.gradleProperty("yap.google.reversedClientId").getOrElse("app.yap.oauth")
    }
}

dependencies {
    implementation(project(":apps:mobile:app-root"))
    implementation(project(":apps:mobile:shared-app"))
    implementation(libs.androidx.core.splashscreen)
}
