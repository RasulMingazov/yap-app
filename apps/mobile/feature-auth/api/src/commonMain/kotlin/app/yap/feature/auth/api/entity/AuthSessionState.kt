package app.yap.feature.auth.api.entity

sealed interface AuthSessionState {

    data object Unknown : AuthSessionState

    data object LoggedOut : AuthSessionState

    data class LoggedIn(val userId: UserId) : AuthSessionState
}
