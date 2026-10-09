package app.yap.feature.scenario.domain.usecase

import app.yap.core.common.analytics.AnalyticsEvent
import app.yap.core.common.analytics.AnalyticsTracker
import app.yap.core.network.ApiError
import app.yap.feature.scenario.api.entity.Scenario
import app.yap.feature.scenario.api.entity.ScenarioStatus
import app.yap.feature.scenario.domain.gateway.PaywallGateway
import app.yap.feature.scenario.domain.gateway.PaywallOrigin
import app.yap.feature.scenario.domain.gateway.PaywallSource
import app.yap.feature.scenario.domain.gateway.SessionGateway
import app.yap.feature.scenario.domain.repository.ActivationResult
import app.yap.feature.scenario.domain.repository.ScenarioRepository
import app.yap.feature.scenario.domain.ScenarioAnalytics

internal interface OpenScenarioUseCase {

    suspend operator fun invoke(scenario: Scenario, origin: PaywallOrigin): OpenScenarioOutcome
}

internal sealed interface OpenScenarioOutcome {

    data object Opened : OpenScenarioOutcome

    data object PaywallShown : OpenScenarioOutcome

    data object SlotLimitReached : OpenScenarioOutcome

    data object NoConnection : OpenScenarioOutcome

    data object Failed : OpenScenarioOutcome
}

internal class DefaultOpenScenarioUseCase(
    private val analyticsTracker: AnalyticsTracker,
    private val paywallGateway: PaywallGateway,
    private val scenarioRepository: ScenarioRepository,
    private val sessionGateway: SessionGateway,
) : OpenScenarioUseCase {

    override suspend fun invoke(scenario: Scenario, origin: PaywallOrigin): OpenScenarioOutcome {
        if (scenario.locked) return openPaywall(scenario, origin)

        return when (scenario.status) {
            is ScenarioStatus.Active -> open(scenario)
            is ScenarioStatus.Available -> activateAndOpen(scenario, origin)
            is ScenarioStatus.Completed -> OpenScenarioOutcome.Failed
        }
    }

    private suspend fun activateAndOpen(scenario: Scenario, origin: PaywallOrigin): OpenScenarioOutcome =
        when (val result = scenarioRepository.activate(scenario.id)) {
            is ActivationResult.Opened -> open(scenario)
            is ActivationResult.AccessRequired -> openPaywall(scenario, origin)
            is ActivationResult.SlotLimitReached -> OpenScenarioOutcome.SlotLimitReached
            is ActivationResult.Failed -> when (result.error) {
                is ApiError.Unavailable -> OpenScenarioOutcome.NoConnection
                else -> OpenScenarioOutcome.Failed
            }
        }

    private suspend fun open(scenario: Scenario): OpenScenarioOutcome {
        analyticsTracker.track(
            AnalyticsEvent(
                name = ScenarioAnalytics.SCENARIO_OPEN,
                params = mapOf(ScenarioAnalytics.PARAM_SCENARIO_ID to scenario.id.value),
            ),
        )
        sessionGateway.open(scenario)
        return OpenScenarioOutcome.Opened
    }

    private suspend fun openPaywall(scenario: Scenario, origin: PaywallOrigin): OpenScenarioOutcome {
        analyticsTracker.track(
            AnalyticsEvent(
                name = ScenarioAnalytics.LOCKED_CONTENT_CLICK,
                params = mapOf(
                    ScenarioAnalytics.PARAM_SCENARIO_ID to scenario.id.value,
                    ScenarioAnalytics.PARAM_ORIGIN to origin.name,
                ),
            ),
        )
        paywallGateway.open(PaywallSource(scenarioId = scenario.id, origin = origin))
        return OpenScenarioOutcome.PaywallShown
    }
}
