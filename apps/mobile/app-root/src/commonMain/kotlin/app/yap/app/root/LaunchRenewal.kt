package app.yap.app.root

import app.yap.feature.auth.api.entity.AuthState
import app.yap.feature.auth.api.usecase.ObserveAuthStateUseCase
import app.yap.feature.auth.api.usecase.RenewSessionUseCase
import kotlinx.coroutines.flow.first

internal class LaunchRenewal(
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val renewSessionUseCase: RenewSessionUseCase,
) {

    suspend fun run() {
        val resolved = observeAuthStateUseCase().first { authState -> authState !is AuthState.Unknown }
        if (resolved !is AuthState.LoggedIn) return

        renewSessionUseCase()
    }
}
