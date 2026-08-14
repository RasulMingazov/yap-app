package app.yap.server.feature.auth.api

import app.yap.contract.auth.GoogleAuthorizationCodeDto
import app.yap.contract.auth.GoogleCredentialsDto
import app.yap.contract.auth.RefreshCredentialsDto
import app.yap.server.feature.auth.AuthService
import app.yap.server.feature.auth.model.AuthFailure
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlin.coroutines.cancellation.CancellationException

internal fun Route.authRoutes(authService: AuthService) {
    route("/v1/auth") {
        post("/google") {
            val credentials = call.receiveOrMalformed<GoogleCredentialsDto>()
            val session = authService.loginWithGoogleIdToken(
                idToken = credentials.idToken,
                nonce = credentials.nonce,
            )
            call.respond(session.toDto())
        }

        post("/google/code") {
            val credentials = call.receiveOrMalformed<GoogleAuthorizationCodeDto>()
            val session = authService.loginWithGoogleAuthorizationCode(
                code = credentials.code,
                codeVerifier = credentials.codeVerifier,
                redirectUri = credentials.redirectUri,
            )
            call.respond(session.toDto())
        }

        post("/refresh") {
            val credentials = call.receiveOrMalformed<RefreshCredentialsDto>()
            val session = authService.rotate(credentials.refreshToken)
            call.respond(session.toDto())
        }
    }
}

@Suppress("TooGenericExceptionCaught")
private suspend inline fun <reified T : Any> io.ktor.server.application.ApplicationCall.receiveOrMalformed(): T =
    try {
        receive<T>()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        throw AuthFailure.MalformedInput()
    }
