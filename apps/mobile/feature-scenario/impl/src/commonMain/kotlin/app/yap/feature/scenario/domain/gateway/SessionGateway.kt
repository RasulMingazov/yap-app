package app.yap.feature.scenario.domain.gateway

import app.yap.feature.scenario.api.entity.Scenario

/** `feature-session` adapts this later; until then `ComingSoonSessionGateway` (research R6). */
internal interface SessionGateway {

    suspend fun open(scenario: Scenario)
}
