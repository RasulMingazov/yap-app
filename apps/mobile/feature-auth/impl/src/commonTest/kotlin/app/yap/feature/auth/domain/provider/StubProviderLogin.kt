package app.yap.feature.auth.domain.provider

import app.yap.feature.auth.api.entity.AuthProviderType
import app.yap.feature.auth.api.entity.LoginOutcome
import io.github.rasulmingazov.stubcall.StubCall0
import kotlinx.coroutines.CompletableDeferred

internal class StubProviderLogin(
    override val type: AuthProviderType = AuthProviderType.GOOGLE,
    outcome: LoginOutcome = LoginOutcome.Success,
    private val gate: CompletableDeferred<Unit>? = null,
) : ProviderLogin {

    val loginCall = StubCall0.returns(outcome)

    override suspend fun login(): LoginOutcome {
        gate?.await()
        return loginCall.invoke()
    }
}
