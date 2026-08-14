package app.yap.server.app

import app.yap.server.feature.auth.model.AuthFailure
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

internal fun Application.installErrorMapping() {
    install(StatusPages) {
        exception<AuthFailure> { call, failure ->
            val (status, code) = when (failure) {
                is AuthFailure.MalformedInput -> HttpStatusCode.BadRequest to "invalid_request"
                is AuthFailure.UnverifiableConfirmation -> HttpStatusCode.Unauthorized to "unauthorized"
                is AuthFailure.ProviderUnavailable ->
                    HttpStatusCode.ServiceUnavailable to "provider_unavailable"
            }
            call.respond(status, ErrorResponse(error = code))
        }
    }
}
