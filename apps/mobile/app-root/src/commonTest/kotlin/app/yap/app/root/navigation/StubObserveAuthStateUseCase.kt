package app.yap.app.root.navigation

import app.yap.feature.auth.api.entity.AuthState
import app.yap.feature.auth.api.usecase.ObserveAuthStateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class StubObserveAuthStateUseCase(
    authState: AuthState = AuthState.Unknown,
) : ObserveAuthStateUseCase {

    val authStates = MutableStateFlow(authState)

    override fun invoke(): Flow<AuthState> = authStates
}
