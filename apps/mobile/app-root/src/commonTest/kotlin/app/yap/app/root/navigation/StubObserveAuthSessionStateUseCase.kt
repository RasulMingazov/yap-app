package app.yap.app.root.navigation

import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.auth.api.usecase.ObserveAuthSessionStateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class StubObserveAuthSessionStateUseCase(
    authSessionState: AuthSessionState = AuthSessionState.Unknown,
) : ObserveAuthSessionStateUseCase {

    val authSessionStates = MutableStateFlow(authSessionState)

    override fun invoke(): Flow<AuthSessionState> = authSessionStates
}
