package app.yap.feature.auth.domain.repository

import app.yap.feature.auth.api.entity.AuthSessionState
import io.github.rasulmingazov.stubcall.StubCall0
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class StubAuthSessionRepository(
    authSessionState: AuthSessionState = AuthSessionState.Unknown,
) : AuthSessionRepository {

    private val state = MutableStateFlow(authSessionState)

    val refreshCall = StubCall0.unit()

    override fun observe(): Flow<AuthSessionState> = state.asStateFlow()

    override suspend fun refresh() = refreshCall.invoke()
}
