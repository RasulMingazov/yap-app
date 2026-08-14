package app.yap.feature.auth.api.entity

sealed interface LoginOutcome {

    data object Success : LoginOutcome

    data object Cancelled : LoginOutcome

    data object Failed : LoginOutcome

    data object Unavailable : LoginOutcome
}
