package app.yap.feature.auth.domain.usecase

import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.api.usecase.LoginUseCase
import io.github.rasulmingazov.stubcall.StubCall1
import kotlinx.coroutines.CompletableDeferred

internal class StubLoginUseCase(
    outcome: LoginOutcome = LoginOutcome.Success,
    private val gate: CompletableDeferred<Unit>? = null,
) : LoginUseCase {

    val invokeCall = StubCall1.returns<AuthProvider, LoginOutcome>(outcome)

    override suspend fun invoke(provider: AuthProvider): LoginOutcome {
        gate?.await()
        return invokeCall.invoke(provider)
    }
}
