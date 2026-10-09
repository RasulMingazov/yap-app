package app.yap.feature.scenario.placeholder

import app.yap.core.common.navigation.Navigator
import app.yap.feature.scenario.domain.gateway.PaywallGateway
import app.yap.feature.scenario.domain.gateway.PaywallSource

/**
 * The paywall belongs to `feature-subscription`; the source already carries the scenario to
 * resume after a purchase, so the real adapter swaps in without touching callers (US4).
 */
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
