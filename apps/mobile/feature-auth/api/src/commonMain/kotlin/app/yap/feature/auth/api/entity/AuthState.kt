package app.yap.feature.auth.api.entity

sealed interface AuthState {

    data object Unknown : AuthState

    data object LoggedOut : AuthState

    data class LoggedIn(val userId: UserId) : AuthState
}
