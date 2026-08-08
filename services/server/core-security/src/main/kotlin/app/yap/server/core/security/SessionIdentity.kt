package app.yap.server.core.security

data class SessionIdentity(
    val userId: String,
    val sessionId: String,
)
