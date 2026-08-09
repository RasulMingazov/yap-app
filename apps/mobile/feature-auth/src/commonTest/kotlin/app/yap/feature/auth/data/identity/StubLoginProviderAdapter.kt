package app.yap.feature.auth.data.identity

import app.yap.feature.auth.domain.entity.LoginProviderId
import io.github.rasulmingazov.stubcall.StubCall0
import io.github.rasulmingazov.stubcall.StubCall1
import io.github.rasulmingazov.stubcall.StubCall2

internal class StubLoginProviderAdapter(
    attempt: PreparedAttempt = StubPreparedAttempt.stubPreparedAttempt(),
    result: ProviderAuthResult = ProviderAuthResult.Success(
        credential = StubPreparedAttempt.stubIdentityTokenCredential(),
    ),
    override val providerId: LoginProviderId = LoginProviderId.Google,
    private val journal: MutableList<String> = mutableListOf(),
) : LoginProviderAdapter {

    val authenticateCall = StubCall2.returns<PreparedAttempt, LoginChallenge, ProviderAuthResult>(result)
    val discardCall = StubCall1.unit<PreparedAttempt>()
    val prepareAttemptCall = StubCall0.returns(attempt)

    override suspend fun prepareAttempt(): PreparedAttempt {
        journal += "prepareAttempt"
        return prepareAttemptCall.invoke()
    }

    override suspend fun authenticate(
        attempt: PreparedAttempt,
        challenge: LoginChallenge,
    ): ProviderAuthResult {
        journal += "authenticate"
        return authenticateCall.invoke(attempt, challenge)
    }

    override fun discard(attempt: PreparedAttempt) {
        journal += "discard"
        discardCall.invoke(attempt)
    }
}
