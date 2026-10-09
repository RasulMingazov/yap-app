package app.yap.feature.scenario.domain.gateway

import app.yap.feature.scenario.api.entity.ScenarioId

internal interface PaywallGateway {

    suspend fun open(source: PaywallSource)
}

internal data class PaywallSource(
    val scenarioId: ScenarioId?,
    val origin: PaywallOrigin,
)

internal enum class PaywallOrigin { HeroPromo, LockedPreview, LockedRow }
