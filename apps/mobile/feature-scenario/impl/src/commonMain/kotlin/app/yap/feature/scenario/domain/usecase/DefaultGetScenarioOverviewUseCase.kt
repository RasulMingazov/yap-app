package app.yap.feature.scenario.domain.usecase

import app.yap.feature.scenario.api.entity.ScenarioOverview
import app.yap.feature.scenario.api.usecase.GetScenarioOverviewUseCase
import app.yap.feature.scenario.domain.repository.ScenarioRepository

internal class DefaultGetScenarioOverviewUseCase(
    private val scenarioRepository: ScenarioRepository,
) : GetScenarioOverviewUseCase {

    override suspend fun invoke(forceUpdate: Boolean): ScenarioOverview? = scenarioRepository.get(forceUpdate)
}
