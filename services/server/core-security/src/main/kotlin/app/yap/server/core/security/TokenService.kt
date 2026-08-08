package app.yap.server.core.security

interface TokenService {
    fun createChallenge(ttlSeconds: Long): SecurityChallenge

    fun createRefreshToken(): RefreshToken

    fun rotateRefreshToken(token: RefreshToken): RefreshToken

    fun parseRefreshToken(value: String): RefreshToken

    fun hash(value: String): String

    fun issueTokens(
        session: SessionIdentity,
        refreshToken: RefreshToken,
    ): IssuedTokens
}
