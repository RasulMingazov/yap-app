package app.yap.server.core.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID

class JwtTokenService(
    jwtSecret: String,
    private val jwtIssuer: String,
    private val jwtAudience: String,
    private val accessTokenTtlSeconds: Long,
    private val clock: Clock = Clock.systemUTC(),
    private val secureRandom: SecureRandom = SecureRandom(),
) : TokenService {
    private val algorithm = Algorithm.HMAC256(jwtSecret)

    override fun createChallenge(ttlSeconds: Long): SecurityChallenge = SecurityChallenge(
        id = UUID.randomUUID().toString(),
        nonce = randomUrlSafeValue(),
        expiresAt = Instant.now(clock).plusSeconds(ttlSeconds),
    )

    override fun createRefreshToken(): RefreshToken {
        val sessionId = UUID.randomUUID().toString()
        return RefreshToken(
            sessionId = sessionId,
            value = createRefreshToken(
                sessionId = sessionId,
                secret = randomUrlSafeValue(),
            ),
        )
    }

    override fun rotateRefreshToken(token: RefreshToken): RefreshToken =
        RefreshToken(
            sessionId = token.sessionId,
            value = createRefreshToken(
                sessionId = token.sessionId,
                secret = randomUrlSafeValue(),
            ),
        )

    override fun parseRefreshToken(value: String): RefreshToken {
        val sessionId = parseSessionId(value) ?: throw InvalidTokenException()
        return RefreshToken(
            sessionId = sessionId,
            value = value,
        )
    }

    private fun parseSessionId(value: String): String? =
        value.takeIf { it.startsWith(REFRESH_PREFIX) }
            ?.removePrefix(REFRESH_PREFIX)
            ?.split('.', limit = 2)
            ?.takeIf { it.size == 2 && it.none(String::isBlank) }
            ?.first()
            ?.takeIf { runCatching { UUID.fromString(it) }.isSuccess }

    override fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    override fun issueTokens(
        session: SessionIdentity,
        refreshToken: RefreshToken,
    ): IssuedTokens {
        val issuedAt = Instant.now(clock)
        val expiresAt = issuedAt.plusSeconds(accessTokenTtlSeconds)
        val accessToken = JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withSubject(session.userId)
            .withJWTId(UUID.randomUUID().toString())
            .withClaim(SESSION_ID_CLAIM, session.sessionId)
            .withClaim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
            .withIssuedAt(Date.from(issuedAt))
            .withExpiresAt(Date.from(expiresAt))
            .sign(algorithm)
        return IssuedTokens(
            accessToken = accessToken,
            refreshToken = refreshToken.value,
            accessTokenExpiresAtEpochSeconds = expiresAt.epochSecond,
        )
    }

    private fun randomUrlSafeValue(): String = ByteArray(RANDOM_VALUE_SIZE)
        .also(secureRandom::nextBytes)
        .let(Base64.getUrlEncoder().withoutPadding()::encodeToString)

    private fun createRefreshToken(sessionId: String, secret: String): String =
        "$REFRESH_PREFIX$sessionId.$secret"

    companion object {
        const val SESSION_ID_CLAIM = "sid"
        const val TOKEN_TYPE_CLAIM = "type"
        const val ACCESS_TOKEN_TYPE = "access"
        private const val REFRESH_PREFIX = "ysr_"
        private const val RANDOM_VALUE_SIZE = 32
    }
}
