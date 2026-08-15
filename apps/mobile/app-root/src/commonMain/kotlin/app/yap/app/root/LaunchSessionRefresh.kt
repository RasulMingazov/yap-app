package app.yap.app.root

import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.auth.api.usecase.ObserveAuthSessionStateUseCase
import app.yap.feature.auth.api.usecase.RefreshSessionUseCase
import kotlinx.coroutines.flow.first

internal class LaunchSessionRefresh(
    private val observeAuthSessionStateUseCase: ObserveAuthSessionStateUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
) {

    suspend fun run() {
        val resolved = observeAuthSessionStateUseCase().first { authSessionState -> authSessionState !is AuthSessionState.Unknown }
        if (resolved !is AuthSessionState.LoggedIn) return

        refreshSessionUseCase()
    }
}
