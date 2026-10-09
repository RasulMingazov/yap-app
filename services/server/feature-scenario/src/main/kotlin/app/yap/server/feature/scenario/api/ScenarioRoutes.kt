package app.yap.server.feature.scenario.api

import app.yap.contract.scenario.ReportProgressRequestDto
import app.yap.server.core.security.InvalidTokenException
import app.yap.server.core.security.TokenService
import app.yap.server.feature.scenario.ScenarioService
import app.yap.server.feature.scenario.model.ProgressReport
import app.yap.server.feature.scenario.model.ScenarioFailure
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

private const val BEARER_PREFIX = "Bearer "

internal fun Route.scenarioRoutes(scenarioService: ScenarioService, tokenService: TokenService) {
    route("/v1/scenarios") {
        get("/state") {
            val userId = call.authenticatedUserId(tokenService)
            call.respond(scenarioService.state(userId = userId).toDto())
        }

        post("/{id}/activate") {
            val userId = call.authenticatedUserId(tokenService)
            val state = scenarioService.activate(scenarioId = call.scenarioId(), userId = userId)
            call.respond(state.toDto())
        }

        post("/{id}/repeat") {
            val userId = call.authenticatedUserId(tokenService)
            val state = scenarioService.repeat(scenarioId = call.scenarioId(), userId = userId)
            call.respond(state.toDto())
        }

        post("/{id}/progress") {
            val userId = call.authenticatedUserId(tokenService)
            val report = call.receiveOrMalformed<ReportProgressRequestDto>().toModel()
            val state = scenarioService.report(
                report = report,
                scenarioId = call.scenarioId(),
                userId = userId,
            )
            call.respond(state.toDto())
        }
    }
}

private fun ApplicationCall.authenticatedUserId(tokenService: TokenService): UUID {
    val token = request.headers[HttpHeaders.Authorization]
        ?.takeIf { header -> header.startsWith(BEARER_PREFIX) }
        ?.removePrefix(BEARER_PREFIX)

    val identity = token?.let {
        try {
            tokenService.verifyAccessToken(it)
        } catch (_: InvalidTokenException) {
            null
        }
    }
    return identity?.let { runCatching { UUID.fromString(it.userId) }.getOrNull() }
        ?: throw ScenarioFailure.Unauthorized()
}

private fun ApplicationCall.scenarioId(): String =
    parameters["id"] ?: throw ScenarioFailure.MalformedInput()

private fun ReportProgressRequestDto.toModel(): ProgressReport = try {
    ProgressReport(
        reportId = UUID.fromString(reportId),
        attempt = attempt,
        achievedObjective = achievedObjective,
        elapsedSeconds = elapsedSeconds,
    )
} catch (_: IllegalArgumentException) {
    throw ScenarioFailure.MalformedInput()
}

@Suppress("TooGenericExceptionCaught")
private suspend inline fun <reified T : Any> ApplicationCall.receiveOrMalformed(): T =
    try {
        receive<T>()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        throw ScenarioFailure.MalformedInput()
    }
