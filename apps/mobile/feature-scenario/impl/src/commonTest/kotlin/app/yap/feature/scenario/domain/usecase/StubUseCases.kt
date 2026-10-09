package app.yap.feature.scenario.domain.usecase

import app.yap.feature.scenario.api.entity.OverviewState
import app.yap.feature.scenario.api.entity.Scenario
import app.yap.feature.scenario.api.usecase.ObserveScenarioOverviewUseCase
import app.yap.feature.scenario.domain.gateway.PaywallOrigin
import app.yap.feature.scenario.domain.gateway.PaywallSource
import io.github.rasulmingazov.stubcall.StubCall0
import io.github.rasulmingazov.stubcall.StubCall1
import io.github.rasulmingazov.stubcall.StubCall2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class StubObserveScenarioOverviewUseCase(
    state: OverviewState = OverviewState.Loading,
) : ObserveScenarioOverviewUseCase {

    val states = MutableStateFlow(state)

    override fun invoke(): Flow<OverviewState> = states
}

internal class StubRefreshOverviewUseCase : RefreshOverviewUseCase {

    val invokeCall = StubCall0.unit()

    override suspend fun invoke() = invokeCall.invoke()
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
