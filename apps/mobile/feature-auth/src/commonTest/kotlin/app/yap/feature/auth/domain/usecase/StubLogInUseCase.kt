package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.domain.entity.LoginOutcome
import app.yap.feature.auth.domain.entity.LoginProviderId
import io.github.rasulmingazov.stubcall.StubCall1

internal class StubLogInUseCase(
    outcome: LoginOutcome = LoginOutcome.Cancelled,
) : LogInUseCase {

    val invokeCall = StubCall1.returns<LoginProviderId, LoginOutcome>(outcome)

    override suspend fun invoke(providerId: LoginProviderId): LoginOutcome = invokeCall.invoke(providerId)
}
