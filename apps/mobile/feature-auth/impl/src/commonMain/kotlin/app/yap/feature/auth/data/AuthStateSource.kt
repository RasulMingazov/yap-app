package app.yap.feature.auth.data

import app.yap.feature.auth.api.entity.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class AuthStateSource {

    private val state = MutableStateFlow<AuthState>(AuthState.Unknown)

    val authState: StateFlow<AuthState> = state.asStateFlow()

    fun publish(authState: AuthState) {
        state.value = authState
    }
}
