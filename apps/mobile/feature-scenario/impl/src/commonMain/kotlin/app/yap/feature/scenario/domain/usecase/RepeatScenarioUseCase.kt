package app.yap.feature.scenario.domain.usecase

import app.yap.core.common.analytics.AnalyticsEvent
import app.yap.core.common.analytics.AnalyticsTracker
import app.yap.core.network.ApiError
import app.yap.feature.scenario.api.entity.Scenario
import app.yap.feature.scenario.domain.ScenarioAnalytics
import app.yap.feature.scenario.domain.gateway.PaywallGateway
import app.yap.feature.scenario.domain.gateway.PaywallOrigin
import app.yap.feature.scenario.domain.gateway.PaywallSource
import app.yap.feature.scenario.domain.gateway.SessionGateway
import app.yap.feature.scenario.domain.repository.ActivationResult
import app.yap.feature.scenario.domain.repository.ScenarioRepository

internal interface RepeatScenarioUseCase {

    suspend operator fun invoke(scenario: Scenario): OpenScenarioOutcome
}

/** The confirmed restart (FR-034): attempt + 1 on the server, then straight into practice. */
internal class DefaultRepeatScenarioUseCase(
    private val analyticsTracker: AnalyticsTracker,
    private val paywallGateway: PaywallGateway,
    private val scenarioRepository: ScenarioRepository,
    private val sessionGateway: SessionGateway,
) : RepeatScenarioUseCase {

    override suspend fun invoke(scenario: Scenario): OpenScenarioOutcome =
        when (val result = scenarioRepository.repeat(scenario.id)) {
            is ActivationResult.Opened -> {
                analyticsTracker.track(
                    AnalyticsEvent(
                        name = ScenarioAnalytics.SCENARIO_OPEN,
                        params = mapOf(ScenarioAnalytics.PARAM_SCENARIO_ID to scenario.id.value),
                    ),
                )
                sessionGateway.open(scenario)
                OpenScenarioOutcome.Opened
            }
            is ActivationResult.AccessRequired -> {
                paywallGateway.open(
                    PaywallSource(scenarioId = scenario.id, origin = PaywallOrigin.LockedRow),
                )
                OpenScenarioOutcome.PaywallShown
            }
            is ActivationResult.SlotLimitReached -> OpenScenarioOutcome.SlotLimitReached
            is ActivationResult.Failed -> when (result.error) {
                is ApiError.Unavailable -> OpenScenarioOutcome.NoConnection
                else -> OpenScenarioOutcome.Failed
            }
        }
}
