package app.yap.feature.scenario.domain.usecase

import app.yap.feature.scenario.StubAnalyticsTracker
import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.api.entity.ScenarioStatus
import app.yap.feature.scenario.api.entity.StubScenarioOverview
import app.yap.feature.scenario.domain.ScenarioAnalytics
import app.yap.feature.scenario.domain.gateway.StubPaywallGateway
import app.yap.feature.scenario.domain.gateway.StubSessionGateway
import app.yap.feature.scenario.domain.repository.ActivationResult
import app.yap.feature.scenario.domain.repository.StubScenarioRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

internal class RepeatScenarioUseCaseTest {

    @Test
    fun `GIVEN a confirmed restart WHEN it runs THEN the repeat lands first and the session opens`() = runTest {
        val env = Environment()
        val scenario = StubScenarioOverview.stubFreeScenario(status = ScenarioStatus.Completed)

        val outcome = env.useCase(scenario)

        assertIs<OpenScenarioOutcome.Opened>(outcome)
        env.repository.repeatCall.calledWith(ScenarioId(StubScenarioOverview.FREE_ID))
        env.sessionGateway.openCall.calledWith(scenario)
        assertEquals(expected = listOf(ScenarioAnalytics.SCENARIO_OPEN), actual = env.analyticsTracker.names())
    }

    @Test
    fun `GIVEN no free slot WHEN a restart is confirmed THEN the limit refusal reaches the caller and nothing opens`() =
        runTest {
            val env = Environment()
            env.repository.repeatCall.returns(ActivationResult.SlotLimitReached)

            val outcome = env.useCase(StubScenarioOverview.stubFreeScenario(status = ScenarioStatus.Completed))

            assertIs<OpenScenarioOutcome.SlotLimitReached>(outcome)
            env.sessionGateway.openCall.notCalled()
        }

    private class Environment {

        val analyticsTracker = StubAnalyticsTracker()
        val paywallGateway = StubPaywallGateway()
        val repository = StubScenarioRepository()
        val sessionGateway = StubSessionGateway()
        val useCase: RepeatScenarioUseCase = DefaultRepeatScenarioUseCase(
            analyticsTracker = analyticsTracker,
            paywallGateway = paywallGateway,
            scenarioRepository = repository,
            sessionGateway = sessionGateway,
        )
    }
}
