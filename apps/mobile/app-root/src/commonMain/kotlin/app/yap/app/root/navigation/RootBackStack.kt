package app.yap.app.root.navigation

import androidx.navigation3.runtime.NavKey
import app.yap.core.common.navigation.Navigator
import app.yap.feature.auth.api.AuthNavKey
import app.yap.feature.auth.api.entity.AuthState
import app.yap.feature.auth.api.usecase.ObserveAuthStateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class RootBackStack(
    observeAuthStateUseCase: ObserveAuthStateUseCase,
) : Navigator {

    private val tail = MutableStateFlow<List<NavKey>>(emptyList())

    private var pushedOnto: List<NavKey>? = null

    val keys: Flow<List<NavKey>> = observeAuthStateUseCase()
        .map(::rootKeys)
        .distinctUntilChanged()
        .onEach(::dropTailIfBaseChanged)
        .combine(tail) { base, pushed -> if (base.isEmpty()) base else base + pushed }
        .distinctUntilChanged()

    override fun navigate(key: NavKey) {
        tail.update { pushed -> if (pushed.lastOrNull() == key) pushed else pushed + key }
    }

    private fun dropTailIfBaseChanged(base: List<NavKey>) {
        val previous = pushedOnto
        pushedOnto = base
        if (previous != null && previous != base) tail.value = emptyList()
    }

    override fun back() {
        tail.update { pushed -> pushed.dropLast(1) }
    }

    private fun rootKeys(authState: AuthState): List<NavKey> = when (authState) {
        is AuthState.Unknown -> emptyList()
        is AuthState.LoggedOut -> listOf(AuthNavKey.Login)
        is AuthState.LoggedIn -> listOf(RootNavKey.Main)
    }
}
