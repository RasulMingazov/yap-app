package app.yap.server.feature.auth

import app.yap.server.feature.auth.model.AuthAccount
import app.yap.server.feature.auth.model.AuthChallenge
import app.yap.server.feature.auth.model.NewSession
import app.yap.server.feature.auth.model.VerifiedIdentity
import app.yap.server.feature.auth.persistence.AuthRepository
import io.github.rasulmingazov.stubcall.StubCall1
import io.github.rasulmingazov.stubcall.StubCall3

internal class StubAuthRepository(
    account: AuthAccount? = StubAuthAccount.stubAuthAccount(),
    challenge: AuthChallenge? = StubAuthChallenge.stubAuthChallenge(),
) : AuthRepository {

    val consumeChallengeAndCreateSessionCall =
        StubCall3.returns<AuthChallenge, VerifiedIdentity, NewSession, AuthAccount?>(account)
    val findChallengeCall = StubCall1.returns<String, AuthChallenge?>(challenge)
    val insertChallengeCall = StubCall1.unit<AuthChallenge>()

    override suspend fun consumeChallengeAndCreateSession(
        challenge: AuthChallenge,
        identity: VerifiedIdentity,
        session: NewSession,
    ): AuthAccount? = consumeChallengeAndCreateSessionCall.invoke(challenge, identity, session)

    override suspend fun findChallenge(id: String): AuthChallenge? = findChallengeCall.invoke(id)

    override suspend fun insertChallenge(challenge: AuthChallenge) =
        insertChallengeCall.invoke(challenge)
}
