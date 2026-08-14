package app.yap.feature.auth.domain.repository

import app.yap.feature.auth.api.entity.AuthState
import app.yap.feature.auth.api.entity.LoginOutcome
import io.github.rasulmingazov.stubcall.StubCall0
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class StubAuthRepository(
    authState: AuthState = AuthState.Unknown,
    accessTokenLifetimeSeconds: Long? = null,
    outcome: LoginOutcome = LoginOutcome.Success,
) : AuthRepository {

    private val state = MutableStateFlow(authState)

    val accessTokenLifetimeSecondsCall = StubCall0.returns(accessTokenLifetimeSeconds)
    val loginWithGoogleCall = StubCall0.returns(outcome)
    val renewSessionCall = StubCall0.unit()

    override fun observe(): Flow<AuthState> = state.asStateFlow()

    override suspend fun loginWithGoogle(): LoginOutcome = loginWithGoogleCall.invoke()

    override suspend fun accessTokenLifetimeSeconds(): Long? = accessTokenLifetimeSecondsCall.invoke()

    override suspend fun renewSession() = renewSessionCall.invoke()
}
