package app.yap.server.feature.auth

import app.yap.server.feature.auth.model.AuthAccount
import app.yap.server.feature.auth.model.AuthChallenge
import app.yap.server.feature.auth.model.NewSession
import app.yap.server.feature.auth.model.SessionRotation
import app.yap.server.feature.auth.model.SessionRotationResult
import app.yap.server.feature.auth.model.VerifiedIdentity
import app.yap.server.feature.auth.persistence.AuthRepository
import io.github.rasulmingazov.stubcall.StubCall0
import io.github.rasulmingazov.stubcall.StubCall1
import io.github.rasulmingazov.stubcall.StubCall3

internal class StubAuthRepository(
    account: AuthAccount? = StubAuthAccount.stubAuthAccount(),
    challenge: AuthChallenge? = StubAuthChallenge.stubAuthChallenge(),
    expiredChallenges: Int = 0,
    rotation: SessionRotationResult = SessionRotationResult.Rotated(StubAuthAccount.ACCOUNT_ID),
) : AuthRepository {

    val consumeChallengeAndCreateSessionCall =
        StubCall3.returns<AuthChallenge, VerifiedIdentity, NewSession, AuthAccount?>(account)
    val deleteExpiredChallengesCall = StubCall0.returns(expiredChallenges)
    val findChallengeCall = StubCall1.returns<String, AuthChallenge?>(challenge)
    val insertChallengeCall = StubCall1.unit<AuthChallenge>()
    val rotateSessionCall = StubCall1.returns<SessionRotation, SessionRotationResult>(rotation)

    override suspend fun consumeChallengeAndCreateSession(
        challenge: AuthChallenge,
        identity: VerifiedIdentity,
        session: NewSession,
    ): AuthAccount? = consumeChallengeAndCreateSessionCall.invoke(challenge, identity, session)

    override suspend fun deleteExpiredChallenges(): Int = deleteExpiredChallengesCall.invoke()

    override suspend fun findChallenge(id: String): AuthChallenge? = findChallengeCall.invoke(id)

    override suspend fun insertChallenge(challenge: AuthChallenge) =
        insertChallengeCall.invoke(challenge)

    override suspend fun rotateSession(rotation: SessionRotation): SessionRotationResult =
        rotateSessionCall.invoke(rotation)
}
