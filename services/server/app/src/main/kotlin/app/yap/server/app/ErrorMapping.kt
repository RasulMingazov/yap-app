package app.yap.server.app

import app.yap.contract.common.ApiErrorCode
import app.yap.contract.common.ErrorResponseDto
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
                is AuthFailure.MalformedInput -> HttpStatusCode.BadRequest to ApiErrorCode.INVALID_REQUEST
                is AuthFailure.UnverifiableConfirmation -> HttpStatusCode.Unauthorized to ApiErrorCode.UNAUTHORIZED
                is AuthFailure.ProviderUnavailable ->
                    HttpStatusCode.ServiceUnavailable to ApiErrorCode.PROVIDER_UNAVAILABLE
            }
            call.respond(status, ErrorResponseDto(error = code))
        }
    }
}
