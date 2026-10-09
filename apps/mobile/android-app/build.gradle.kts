plugins {
    alias(libs.plugins.yap.android.application)
}

android {
    defaultConfig {
        // AppAuth's `RedirectUriReceiverActivity` declares this placeholder in its own manifest. The
        // redirect URI is the reversed Android client ID (research.md R14); override it with
        // `yap.google.reversedClientId` in `local.properties` or on the command line.
        manifestPlaceholders["appAuthRedirectScheme"] =
            providers.gradleProperty("yap.google.reversedClientId")
                .getOrElse("com.googleusercontent.apps.184232410595-18172vhn52tif6moennk14240qgvh2qv")
    }
}

dependencies {
    implementation(project(":apps:mobile:app-root"))
    implementation(project(":apps:mobile:shared-app"))
    implementation(libs.androidx.core.splashscreen)
}
