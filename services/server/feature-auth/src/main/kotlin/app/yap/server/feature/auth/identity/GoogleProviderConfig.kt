package app.yap.server.feature.auth.identity

/**
 * Feature-owned Google configuration. It is read from the environment, and its absence simply
 * leaves Google unregistered: a login naming an unregistered provider fails as unavailable rather
 * than as a product-level "coming soon" outcome.
 *
 * [fallbackRedirectUri] is the single registered redirect URI of the Android browser fallback. The
 * client sends the URI it actually used and it is compared verbatim before any token exchange.
 */
internal data class GoogleProviderConfig(
    val androidClientId: String?,
    val fallbackRedirectUri: String?,
    val iosClientId: String?,
    val serverClientId: String,
) {

    init {
        require(serverClientId.isNotBlank()) { "$SERVER_CLIENT_ID_VARIABLE must not be blank" }
        require(androidClientId == null || androidClientId.isNotBlank()) {
            "$ANDROID_CLIENT_ID_VARIABLE must not be blank"
        }
        require(iosClientId == null || iosClientId.isNotBlank()) {
            "$IOS_CLIENT_ID_VARIABLE must not be blank"
        }
        require(fallbackRedirectUri == null || fallbackRedirectUri.isNotBlank()) {
            "$FALLBACK_REDIRECT_URI_VARIABLE must not be blank"
        }
    }

    /** The `azp` values Google may issue for this application. */
    val allowedAuthorizedParties: Set<String> =
        setOfNotNull(androidClientId, iosClientId, serverClientId)

    companion object {

        const val ANDROID_CLIENT_ID_VARIABLE = "GOOGLE_ANDROID_CLIENT_ID"
        const val FALLBACK_REDIRECT_URI_VARIABLE = "GOOGLE_FALLBACK_REDIRECT_URI"
        const val IOS_CLIENT_ID_VARIABLE = "GOOGLE_IOS_CLIENT_ID"
        const val SERVER_CLIENT_ID_VARIABLE = "GOOGLE_SERVER_CLIENT_ID"

        /**
         * Returns `null` when Google is not configured at all. A present but invalid value is a
         * configuration error and fails loudly at startup.
         */
        fun fromEnvironment(readVariable: (String) -> String?): GoogleProviderConfig? {
            val serverClientId = readVariable(SERVER_CLIENT_ID_VARIABLE) ?: return null
            return GoogleProviderConfig(
                androidClientId = readVariable(ANDROID_CLIENT_ID_VARIABLE),
                fallbackRedirectUri = readVariable(FALLBACK_REDIRECT_URI_VARIABLE),
                iosClientId = readVariable(IOS_CLIENT_ID_VARIABLE),
                serverClientId = serverClientId,
            )
        }
    }
}
