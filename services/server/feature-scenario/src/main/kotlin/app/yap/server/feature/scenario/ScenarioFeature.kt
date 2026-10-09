package app.yap.server.feature.scenario

import app.yap.server.core.security.TokenService
import app.yap.server.feature.scenario.access.AccessPolicy
import app.yap.server.feature.scenario.api.scenarioRoutes
import app.yap.server.feature.scenario.persistence.ScenarioRepository
import io.ktor.server.routing.Route
import java.time.Clock

class ScenarioFeature(
    accessPolicy: AccessPolicy,
    private val tokenService: TokenService,
) {

    private val scenarioService = ScenarioService(
        accessPolicy = accessPolicy,
        clock = Clock.systemUTC(),
        scenarioRepository = ScenarioRepository(),
    )

    fun install(route: Route) {
        route.scenarioRoutes(scenarioService = scenarioService, tokenService = tokenService)
    }
}
