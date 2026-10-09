package app.yap.feature.scenario.domain.usecase

import app.yap.feature.scenario.api.entity.Scenario
import app.yap.feature.scenario.api.entity.ScenarioOverview
import app.yap.feature.scenario.api.entity.StubScenarioOverview
import app.yap.feature.scenario.api.usecase.GetScenarioOverviewUseCase
import app.yap.feature.scenario.api.usecase.ObserveScenarioOverviewUseCase
import app.yap.feature.scenario.domain.gateway.PaywallOrigin
import app.yap.feature.scenario.domain.gateway.PaywallSource
import io.github.rasulmingazov.stubcall.StubCall1
import io.github.rasulmingazov.stubcall.StubCall2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class StubObserveScenarioOverviewUseCase(
    overview: ScenarioOverview? = null,
) : ObserveScenarioOverviewUseCase {

    val overviews = MutableStateFlow(overview)

    override fun invoke(): Flow<ScenarioOverview?> = overviews
}

internal class StubGetScenarioOverviewUseCase(
    overview: ScenarioOverview? = StubScenarioOverview.stubOverview(),
) : GetScenarioOverviewUseCase {

    val invokeCall = StubCall1.returns<Boolean, ScenarioOverview?>(overview)

    override suspend fun invoke(forceUpdate: Boolean): ScenarioOverview? = invokeCall.invoke(forceUpdate)
}

internal class StubOpenScenarioUseCase(
    outcome: OpenScenarioOutcome = OpenScenarioOutcome.Opened,
) : OpenScenarioUseCase {

    val invokeCall = StubCall2.returns<Scenario, PaywallOrigin, OpenScenarioOutcome>(outcome)

    override suspend fun invoke(scenario: Scenario, origin: PaywallOrigin): OpenScenarioOutcome =
        invokeCall.invoke(scenario, origin)
}

internal class StubPaywallUseCase : OpenPaywallUseCase {

    val invokeCall = StubCall1.unit<PaywallSource>()

    override suspend fun invoke(source: PaywallSource) = invokeCall.invoke(source)
}

internal class StubRepeatScenarioUseCase(
    outcome: OpenScenarioOutcome = OpenScenarioOutcome.Opened,
) : RepeatScenarioUseCase {

    val invokeCall = StubCall1.returns<Scenario, OpenScenarioOutcome>(outcome)

    override suspend fun invoke(scenario: Scenario): OpenScenarioOutcome = invokeCall.invoke(scenario)
}
