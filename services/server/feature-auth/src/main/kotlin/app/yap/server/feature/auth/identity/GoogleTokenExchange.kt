package app.yap.server.feature.auth.identity

/**
 * Exchanges an authorization code obtained by the Android browser fallback for an identity token.
 * The exchange runs on the server so the client never needs a client secret, and it is reached only
 * after the code verifier has been matched against the persisted challenge proof.
 */
internal fun interface GoogleTokenExchange {

    suspend fun exchange(code: String, codeVerifier: String, redirectUri: String): String
}
