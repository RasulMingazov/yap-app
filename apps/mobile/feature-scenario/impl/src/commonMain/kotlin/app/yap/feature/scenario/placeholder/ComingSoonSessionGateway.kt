package app.yap.feature.scenario.placeholder

import app.yap.core.common.navigation.Navigator
import app.yap.feature.scenario.api.entity.Scenario
import app.yap.feature.scenario.domain.gateway.SessionGateway

internal class ComingSoonSessionGateway(
    private val navigator: Navigator,
) : SessionGateway {

    override suspend fun open(scenario: Scenario) {
        navigator.navigate(SessionComingSoonNavKey)
    }
}
