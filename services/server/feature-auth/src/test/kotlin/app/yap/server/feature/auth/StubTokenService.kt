package app.yap.server.feature.auth

import app.yap.server.core.security.IssuedTokens
import app.yap.server.core.security.RefreshToken
import app.yap.server.core.security.SecurityChallenge
import app.yap.server.core.security.SessionIdentity
import app.yap.server.core.security.TokenService
import io.github.rasulmingazov.stubcall.StubCall0
import io.github.rasulmingazov.stubcall.StubCall1
import io.github.rasulmingazov.stubcall.StubCall2

internal class StubTokenService(
    challenge: SecurityChallenge = StubAuthChallenge.stubSecurityChallenge(),
    hash: String = StubAuth.HASH,
    issuedTokens: IssuedTokens = StubAuthSession.stubIssuedTokens(),
    refreshToken: RefreshToken = StubAuthSession.stubRefreshToken(),
) : TokenService {

    val createChallengeCall = StubCall1.returns<Long, SecurityChallenge>(challenge)
    val createRefreshTokenCall = StubCall0.returns(refreshToken)
    val hashCall = StubCall1.returns<String, String>(hash)
    val issueTokensCall = StubCall2.returns<SessionIdentity, RefreshToken, IssuedTokens>(issuedTokens)
    val parseRefreshTokenCall = StubCall1.returns<String, RefreshToken>(refreshToken)
    val rotateRefreshTokenCall = StubCall1.returns<RefreshToken, RefreshToken>(refreshToken)

    override fun createChallenge(ttlSeconds: Long): SecurityChallenge =
        createChallengeCall.invoke(ttlSeconds)

    override fun createRefreshToken(): RefreshToken = createRefreshTokenCall.invoke()

    override fun rotateRefreshToken(token: RefreshToken): RefreshToken =
        rotateRefreshTokenCall.invoke(token)

    override fun parseRefreshToken(value: String): RefreshToken = parseRefreshTokenCall.invoke(value)

    override fun hash(value: String): String = hashCall.invoke(value)

    override fun issueTokens(
        session: SessionIdentity,
        refreshToken: RefreshToken,
    ): IssuedTokens = issueTokensCall.invoke(session, refreshToken)
}
