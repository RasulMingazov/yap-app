package app.yap.feature.auth.domain.repository

import app.yap.feature.auth.api.entity.LoginOutcome
import io.github.rasulmingazov.stubcall.StubCall0

internal class StubGoogleAuthRepository(
    outcome: LoginOutcome = LoginOutcome.Success,
) : GoogleAuthRepository {

    val loginCall = StubCall0.returns(outcome)

    override suspend fun login(): LoginOutcome = loginCall.invoke()
}
