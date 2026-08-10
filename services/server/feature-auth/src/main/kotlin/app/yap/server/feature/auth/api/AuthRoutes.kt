package app.yap.server.feature.auth.api

import app.yap.contract.auth.LoginChallengeRequestDto
import app.yap.contract.auth.LoginRequestDto
import app.yap.contract.auth.RefreshRequestDto
import app.yap.server.feature.auth.AuthService
import app.yap.server.feature.auth.model.AuthFailure
import app.yap.server.feature.auth.model.AuthFailureException
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * The authentication endpoints. They translate HTTP and nothing else: every decision about
 * challenges, credentials, and sessions belongs to [AuthService], and every expected outcome
 * reaches the client through the shared failure mapping.
 *
 * There is deliberately no log-out route. Clearing a session on the device is a client-side
 * reaction to a definitive refresh rejection, so the server has nothing to expose for it.
 */
internal fun Route.authRoutes(service: AuthService) {
    route("/auth") {
        post("/challenge") { call.respondChallenge(service) }
        post("/login") { call.respondLogin(service) }
        post("/refresh") { call.respondRefresh(service) }
    }
}

/**
 * A body whose PKCE fields do not form an accepted combination is rejected before the provider is
 * resolved, so a malformed request never depends on which providers happen to be registered.
 */
private suspend fun ApplicationCall.respondChallenge(service: AuthService) = respondOrFail {
    val request = receiveOrNull<LoginChallengeRequestDto>()
    if (request == null || !request.hasAcceptableCodeChallenge()) throw invalidRequest()

    respond(
        service.startChallenge(
            codeChallenge = request.codeChallenge,
            provider = request.provider,
        ).toDto(),
    )
}

/**
 * A credential shape violation is rejected before the challenge is looked up, so it can never
 * disclose challenge state.
 */
private suspend fun ApplicationCall.respondLogin(service: AuthService) = respondOrFail {
    val request = receiveOrNull<LoginRequestDto>()
    val credential = request?.toLoginCredential() ?: throw invalidRequest()

    respond(
        service.login(
            challengeId = request.challengeId,
            credential = credential,
            provider = request.provider,
        ).toDto(),
    )
}

private suspend fun ApplicationCall.respondRefresh(service: AuthService) = respondOrFail {
    val request = receiveOrNull<RefreshRequestDto>() ?: throw invalidRequest()

    respond(service.refresh(request.refreshToken).toDto())
}

private suspend fun ApplicationCall.respondOrFail(block: suspend () -> Unit) {
    try {
        block()
    } catch (failure: AuthFailureException) {
        respondFailure(failure.failure)
    }
}

/** A body that is not the expected payload at all is a shape violation, not a server failure. */
private suspend inline fun <reified T : Any> ApplicationCall.receiveOrNull(): T? = try {
    receive<T>()
} catch (_: BadRequestException) {
    null
}

private fun invalidRequest(): AuthFailureException = AuthFailureException(AuthFailure.InvalidRequest)
