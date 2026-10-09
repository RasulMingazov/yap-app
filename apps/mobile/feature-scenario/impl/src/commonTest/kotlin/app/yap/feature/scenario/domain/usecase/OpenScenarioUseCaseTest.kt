package app.yap.feature.scenario.domain.usecase

import app.yap.feature.scenario.StubAnalyticsTracker
import app.yap.feature.scenario.api.entity.StubScenarioOverview
import app.yap.feature.scenario.domain.ScenarioAnalytics
import app.yap.feature.scenario.domain.gateway.PaywallOrigin
import app.yap.feature.scenario.domain.gateway.StubPaywallGateway
import app.yap.feature.scenario.domain.gateway.StubSessionGateway
import app.yap.feature.scenario.api.entity.ScenarioId
import app.yap.feature.scenario.domain.repository.ActivationResult
import app.yap.feature.scenario.domain.gateway.PaywallSource
import app.yap.feature.scenario.domain.repository.StubScenarioRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

internal class OpenScenarioUseCaseTest {

    @Test
    fun `GIVEN an active scenario WHEN it is opened THEN the session opens directly and the open is recorded`() =
        runTest {
            val env = Environment()
            val scenario = StubScenarioOverview.stubActiveScenario()

            val outcome = env.useCase(scenario, PaywallOrigin.LockedRow)

            assertIs<OpenScenarioOutcome.Opened>(outcome)
            env.sessionGateway.openCall.calledWith(scenario)
            env.repository.activateCall.notCalled()
            assertEquals(expected = listOf(ScenarioAnalytics.SCENARIO_OPEN), actual = env.analyticsTracker.names())
            assertEquals(
                expected = StubScenarioOverview.FREE_ID,
                actual = env.analyticsTracker.tracked.single().params[ScenarioAnalytics.PARAM_SCENARIO_ID],
            )
        }

    @Test
    fun `GIVEN an available scenario WHEN it is opened THEN it is activated first and the session opens`() =
        runTest {
            val env = Environment()
            val scenario = StubScenarioOverview.stubFreeScenario()

            val outcome = env.useCase(scenario, PaywallOrigin.LockedRow)

            assertIs<OpenScenarioOutcome.Opened>(outcome)
            env.repository.activateCall.calledWith(ScenarioId(StubScenarioOverview.FREE_ID))
            env.sessionGateway.openCall.calledWith(scenario)
        }

    @Test
    fun `GIVEN the limit is reached on the server WHEN activating THEN the outcome names the limit and nothing opens`() =
        runTest {
            val env = Environment()
            env.repository.activateCall.returns(ActivationResult.SlotLimitReached)

            val outcome = env.useCase(StubScenarioOverview.stubFreeScenario(), PaywallOrigin.LockedRow)

            assertIs<OpenScenarioOutcome.SlotLimitReached>(outcome)
            env.sessionGateway.openCall.notCalled()
        }

    @Test
    fun `GIVEN a locked scenario WHEN it is opened THEN the paywall opens and the interest is recorded`() =
        runTest {
            val env = Environment()
            val scenario = StubScenarioOverview.stubLockedScenario()

            val outcome = env.useCase(scenario, PaywallOrigin.LockedPreview)

            assertIs<OpenScenarioOutcome.PaywallShown>(outcome)
            env.paywallGateway.openCall.calledWith(
                PaywallSource(
                    scenarioId = ScenarioId(StubScenarioOverview.LOCKED_ID),
                    origin = PaywallOrigin.LockedPreview,
                ),
            )
            env.sessionGateway.openCall.notCalled()
            val event = env.analyticsTracker.tracked.single()
            assertEquals(expected = ScenarioAnalytics.LOCKED_CONTENT_CLICK, actual = event.name)
            assertEquals(
                expected = StubScenarioOverview.LOCKED_ID,
                actual = event.params[ScenarioAnalytics.PARAM_SCENARIO_ID],
            )
            assertEquals(
                expected = PaywallOrigin.LockedPreview.name,
                actual = event.params[ScenarioAnalytics.PARAM_ORIGIN],
            )
        }

    private class Environment {

        val analyticsTracker = StubAnalyticsTracker()
        val paywallGateway = StubPaywallGateway()
        val repository = StubScenarioRepository()
        val sessionGateway = StubSessionGateway()
        val useCase: OpenScenarioUseCase = DefaultOpenScenarioUseCase(
            analyticsTracker = analyticsTracker,
            paywallGateway = paywallGateway,
            scenarioRepository = repository,
            sessionGateway = sessionGateway,
        )
    }
}
