package app.yap.app.root.navigation

import androidx.navigation3.runtime.NavKey
import app.yap.core.common.navigation.Navigator
import app.yap.core.common.navigation.TabReselects
import app.yap.feature.auth.api.AuthNavKey
import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.auth.api.usecase.ObserveAuthSessionStateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class RootBackStack(
    observeAuthSessionStateUseCase: ObserveAuthSessionStateUseCase,
) : Navigator, TabReselects {

    private val tail = MutableStateFlow<List<NavKey>>(emptyList())

    private val selectedTabState = MutableStateFlow(MainTab.Home)
    val selectedTab: StateFlow<MainTab> = selectedTabState.asStateFlow()

    private val reselectsFlow = MutableSharedFlow<NavKey>(extraBufferCapacity = 1)
    override val reselects: Flow<NavKey> = reselectsFlow.asSharedFlow()

    private var pushedOnto: List<NavKey>? = null

    val keys: Flow<List<NavKey>> = observeAuthSessionStateUseCase()
        .map(::rootKeys)
        .distinctUntilChanged()
        .onEach(::dropTailIfBaseChanged)
        .combine(tail) { base, pushed -> if (base.isEmpty()) base else base + pushed }
        .distinctUntilChanged()

    override fun navigate(key: NavKey) {
        val tab = MainTab.ofKey(key)
        if (tab != null) {
            selectTab(tab)
        } else {
            tail.update { pushed -> if (pushed.lastOrNull() == key) pushed else pushed + key }
        }
    }

    fun selectTab(tab: MainTab) {
        if (selectedTabState.value == tab) {
            reselectsFlow.tryEmit(tab.rootKey)
        } else {
            selectedTabState.value = tab
        }
    }

    private fun dropTailIfBaseChanged(base: List<NavKey>) {
        val previous = pushedOnto
        pushedOnto = base
        if (previous != null && previous != base) {
            tail.value = emptyList()
            selectedTabState.value = MainTab.Home
        }
    }

    override fun back() {
        tail.update { pushed -> pushed.dropLast(1) }
    }

    private fun rootKeys(authSessionState: AuthSessionState): List<NavKey> = when (authSessionState) {
        is AuthSessionState.Unknown -> emptyList()
        is AuthSessionState.LoggedOut -> listOf(AuthNavKey.Login)
        is AuthSessionState.LoggedIn -> listOf(RootNavKey.Main)
    }
}
