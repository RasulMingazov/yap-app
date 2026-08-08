package app.yap.server.core.security

data class RefreshToken(
    val sessionId: String,
    val value: String,
)
