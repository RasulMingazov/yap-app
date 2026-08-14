package app.yap.feature.auth.domain.provider

import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.entity.LoginOutcome
import io.github.rasulmingazov.stubcall.StubCall0
import kotlin.reflect.KClass
import kotlinx.coroutines.CompletableDeferred

internal class StubProviderLogin(
    override val provider: KClass<out AuthProvider> = AuthProvider.Google::class,
    outcome: LoginOutcome = LoginOutcome.Success,
    private val gate: CompletableDeferred<Unit>? = null,
) : ProviderLogin {

    val loginCall = StubCall0.returns(outcome)

    override suspend fun login(): LoginOutcome {
        gate?.await()
        return loginCall.invoke()
    }
}
