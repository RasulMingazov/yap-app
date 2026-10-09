package app.yap.feature.scenario.domain.gateway

import app.yap.feature.scenario.api.entity.ScenarioId

/** `feature-subscription` adapts this later; until then `PlaceholderPaywall` (research R6). */
internal interface PaywallGateway {

    suspend fun open(source: PaywallSource)
}

/**
 * Carries what analytics and the post-purchase continuation need: the future subscription adapter
 * re-opens [scenarioId] after a successful purchase (FR-022, US4).
 */
internal data class PaywallSource(
    val scenarioId: ScenarioId?,
    val origin: PaywallOrigin,
)

internal enum class PaywallOrigin { HeroPromo, LockedPreview, LockedRow }
