package app.yap.server.feature.auth.api

import app.yap.contract.auth.ErrorDto
import app.yap.contract.auth.LoginChallengeDto
import app.yap.contract.auth.SessionDto
import app.yap.server.feature.auth.model.AuthFailure
import app.yap.server.feature.auth.model.IssuedChallenge
import app.yap.server.feature.auth.model.IssuedSession
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

internal fun IssuedChallenge.toDto(): LoginChallengeDto = LoginChallengeDto(
    challengeId = challengeId,
    nonce = nonce,
    expiresAtEpochSeconds = expiresAtEpochSeconds,
)

internal fun IssuedSession.toDto(): SessionDto = SessionDto(
    accessToken = accessToken,
    refreshToken = refreshToken,
    accessTokenExpiresAtEpochSeconds = accessTokenExpiresAtEpochSeconds,
    accountId = accountId,
)

/**
 * The single translation of an authentication outcome to HTTP, shared by every route.
 *
 * Each message is fixed text chosen for its code: an error body never echoes the request, so it can
 * never carry an identity token, an authorization code, a code verifier, or a refresh credential.
 */
internal suspend fun ApplicationCall.respondFailure(failure: AuthFailure) {
    respond(
        status = failure.status(),
        message = ErrorDto(code = failure.code, message = failure.message()),
    )
}

/**
 * A rejected credential is [HttpStatusCode.Unauthorized] whether it was the challenge or the
 * session that failed, and an unregistered provider is a server-side gap rather than a client
 * mistake.
 */
private fun AuthFailure.status(): HttpStatusCode = when (this) {
    AuthFailure.ChallengeInvalid -> HttpStatusCode.Unauthorized
    AuthFailure.InvalidRequest -> HttpStatusCode.BadRequest
    AuthFailure.ProviderUnavailable -> HttpStatusCode.ServiceUnavailable
    AuthFailure.SessionInvalid -> HttpStatusCode.Unauthorized
}

private fun AuthFailure.message(): String = when (this) {
    AuthFailure.ChallengeInvalid -> "The login challenge is not valid."
    AuthFailure.InvalidRequest -> "The request is not a valid authentication request."
    AuthFailure.ProviderUnavailable -> "The identity provider is not available."
    AuthFailure.SessionInvalid -> "The session is no longer valid."
}
