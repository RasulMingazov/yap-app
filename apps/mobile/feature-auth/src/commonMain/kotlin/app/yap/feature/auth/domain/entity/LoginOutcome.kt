package app.yap.feature.auth.domain.entity

internal sealed interface LoginOutcome {

    data object Cancelled : LoginOutcome

    data class Failure(val reason: LoginFailure) : LoginOutcome

    data class Success(val session: Session) : LoginOutcome
}
