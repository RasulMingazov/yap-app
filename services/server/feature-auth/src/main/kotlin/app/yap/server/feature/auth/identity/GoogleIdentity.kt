package app.yap.server.feature.auth.identity

internal data class GoogleIdentity(
    val subject: String,
    val email: String?,
    val displayName: String?,
    val avatarUrl: String?,
)
