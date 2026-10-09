package app.yap.server.feature.scenario.api

import app.yap.contract.common.ApiErrorCode
import app.yap.contract.common.ErrorResponseDto
import app.yap.server.core.security.JwtTokenService
import app.yap.server.core.security.SessionIdentity
import app.yap.server.feature.scenario.ScenarioService
import app.yap.server.feature.scenario.access.AccessPolicy
import app.yap.server.feature.scenario.access.FreeOnlyAccessPolicy
import app.yap.server.feature.scenario.model.ScenarioFailure
import app.yap.server.feature.scenario.persistence.PostgresTestSupport
import app.yap.server.feature.scenario.persistence.PostgresTestSupport.insertUser
import app.yap.server.feature.scenario.persistence.ScenarioRepository
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Assumptions.assumeTrue

internal object ScenarioTestTokens {

    val tokenService = JwtTokenService(
        jwtSecret = "scenario-test-secret-scenario-test-secret",
        jwtIssuer = "yap-test",
        jwtAudience = "yap-mobile-test",
        accessTokenTtlSeconds = 3_600,
    )

    fun bearer(userId: UUID): String {
        val tokens = tokenService.issueTokens(
            session = SessionIdentity(userId = userId.toString(), sessionId = UUID.randomUUID().toString()),
            refreshToken = tokenService.createRefreshToken(),
        )
        return "Bearer ${tokens.accessToken}"
    }
}

internal fun withScenarioApplication(
    accessPolicy: AccessPolicy = FreeOnlyAccessPolicy(),
    block: suspend ApplicationTestBuilder.(DataSource) -> Unit,
) {
    assumeTrue(PostgresTestSupport.isDockerAvailable, "Docker is unavailable — integration suite not run")

    PostgresTestSupport.withDatabase { source ->
        PostgresTestSupport.migrate(source)
        Database.connect(source)
        val scenarioService = ScenarioService(
            accessPolicy = accessPolicy,
            clock = Clock.systemUTC(),
            scenarioRepository = ScenarioRepository(),
        )

        testApplication {
            application {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true; explicitNulls = false })
                }
                // Mirrors the app module's error mapping for the codes this feature answers with.
                install(StatusPages) {
                    exception<ScenarioFailure> { call, failure ->
                        val (status, code) = when (failure) {
                            is ScenarioFailure.MalformedInput ->
                                HttpStatusCode.BadRequest to ApiErrorCode.INVALID_REQUEST
                            is ScenarioFailure.Unauthorized ->
                                HttpStatusCode.Unauthorized to ApiErrorCode.UNAUTHORIZED
                            is ScenarioFailure.NotFound -> HttpStatusCode.NotFound to ApiErrorCode.NOT_FOUND
                            is ScenarioFailure.AccessRequired ->
                                HttpStatusCode.Forbidden to ApiErrorCode.ACCESS_REQUIRED
                            is ScenarioFailure.SlotLimitReached ->
                                HttpStatusCode.Conflict to ApiErrorCode.SLOT_LIMIT_REACHED
                            is ScenarioFailure.NotRepeatable ->
                                HttpStatusCode.Conflict to ApiErrorCode.INVALID_REQUEST
                        }
                        call.respond(status, ErrorResponseDto(error = code))
                    }
                }
                routing {
                    scenarioRoutes(scenarioService = scenarioService, tokenService = ScenarioTestTokens.tokenService)
                }
            }
            block(source)
        }
    }
}

internal fun DataSource.newUser(): UUID = insertUser()
