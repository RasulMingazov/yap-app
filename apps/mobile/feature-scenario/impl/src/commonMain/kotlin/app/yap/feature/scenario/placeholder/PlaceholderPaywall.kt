package app.yap.feature.scenario.placeholder

import app.yap.core.common.navigation.Navigator
import app.yap.feature.scenario.domain.gateway.PaywallGateway
import app.yap.feature.scenario.domain.gateway.PaywallSource

internal class PlaceholderPaywall(
    private val navigator: Navigator,
) : PaywallGateway {

    override suspend fun open(source: PaywallSource) {
        navigator.navigate(
            PlaceholderPaywallNavKey(
                scenarioId = source.scenarioId?.value,
                origin = source.origin.name,
            ),
        )
    }
}
