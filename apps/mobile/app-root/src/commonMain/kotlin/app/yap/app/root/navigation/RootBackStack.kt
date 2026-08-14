package app.yap.app.root.navigation

import androidx.navigation3.runtime.NavKey
import app.yap.feature.auth.api.AuthNavKey
import app.yap.feature.auth.api.entity.AuthState
import app.yap.feature.auth.api.usecase.ObserveAuthStateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RootBackStack(
    observeAuthStateUseCase: ObserveAuthStateUseCase,
) {

    val keys: Flow<List<NavKey>> = observeAuthStateUseCase().map(::rootKeys)

    private fun rootKeys(authState: AuthState): List<NavKey> = when (authState) {
        is AuthState.Unknown -> emptyList()
        is AuthState.LoggedOut -> listOf(AuthNavKey.Login)
        is AuthState.LoggedIn -> listOf(RootNavKey.Main)
    }
}
