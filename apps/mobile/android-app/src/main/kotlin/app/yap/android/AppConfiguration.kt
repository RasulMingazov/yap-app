package app.yap.android

import app.yap.BuildConfig

/**
 * Configuration the entry point owns, mirroring `AppConfiguration` in the iOS host.
 *
 * `build.gradle.kts` fills [BuildConfig] from `local.properties` (see `local.properties.example`).
 * A blank value stops the app here, at startup, with the key to set — not inside a sign-in attempt.
 * The two legal destinations stay `null` until the documents exist: the line renders either way,
 * and the app is not released to users while either is unset.
 */
internal object AppConfiguration {
    val apiBaseUrl: String = required(BuildConfig.API_BASE_URL, "yap.apiBaseUrl")
    val googleWebClientId: String = required(BuildConfig.GOOGLE_WEB_CLIENT_ID, "yap.google.webClientId")
    val googleAndroidClientId: String =
        required(BuildConfig.GOOGLE_ANDROID_CLIENT_ID, "yap.google.androidClientId")
    val googleRedirectUri: String = "${BuildConfig.GOOGLE_REDIRECT_SCHEME}:/oauth2redirect"
    val termsUrl: String? = null
    val privacyUrl: String? = null

    private fun required(value: String, key: String): String {
        check(value.isNotBlank()) {
            "$key is not set: copy local.properties.example to local.properties and fill it in"
        }
        return value
    }
}
