package app.yap.feature.scenario.domain.usecase

import app.yap.feature.scenario.api.entity.ScenarioOverview
import app.yap.feature.scenario.api.usecase.ObserveScenarioOverviewUseCase
import app.yap.feature.scenario.domain.repository.ScenarioRepository
import kotlinx.coroutines.flow.Flow

internal class DefaultObserveScenarioOverviewUseCase(
    private val scenarioRepository: ScenarioRepository,
) : ObserveScenarioOverviewUseCase {

    override fun invoke(): Flow<ScenarioOverview?> = scenarioRepository.observe()
}
