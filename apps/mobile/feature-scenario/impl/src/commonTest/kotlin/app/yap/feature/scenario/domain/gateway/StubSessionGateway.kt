package app.yap.feature.scenario.domain.gateway

import app.yap.feature.scenario.api.entity.Scenario
import io.github.rasulmingazov.stubcall.StubCall1

internal class StubSessionGateway : SessionGateway {

    val openCall = StubCall1.unit<Scenario>()

    override suspend fun open(scenario: Scenario) = openCall.invoke(scenario)
}
