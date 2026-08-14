package app.yap.server.feature.auth.identity

data class GoogleAuthConfig(
    val androidClientId: String,
    val iosClientId: String,
    val webClientId: String,
) {

    val acceptedAudiences: Set<String> =
        setOf(webClientId, androidClientId, iosClientId).filter(String::isNotBlank).toSet()

    init {
        require(acceptedAudiences.isNotEmpty()) {
            "At least one GOOGLE_*_CLIENT_ID must be configured"
        }
    }
}
