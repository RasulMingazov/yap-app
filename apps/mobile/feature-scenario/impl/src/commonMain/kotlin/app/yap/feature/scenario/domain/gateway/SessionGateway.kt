package app.yap.feature.scenario.domain.gateway

import app.yap.feature.scenario.api.entity.Scenario

internal interface SessionGateway {

    suspend fun open(scenario: Scenario)
}
