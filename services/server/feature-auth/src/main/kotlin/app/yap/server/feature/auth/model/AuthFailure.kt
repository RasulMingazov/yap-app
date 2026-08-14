package app.yap.server.feature.auth.model

sealed class AuthFailure(message: String) : RuntimeException(message) {

    class MalformedInput(message: String = "Malformed request") : AuthFailure(message)

    class UnverifiableConfirmation(
        message: String = "Confirmation could not be verified",
    ) : AuthFailure(message)

    class ProviderUnavailable(
        message: String = "Identity provider unavailable",
    ) : AuthFailure(message)
}
