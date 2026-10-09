package app.yap.feature.scenario.api.usecase

import app.yap.feature.scenario.api.entity.ScenarioOverview
import kotlinx.coroutines.flow.Flow

interface ObserveScenarioOverviewUseCase {

    operator fun invoke(): Flow<ScenarioOverview?>
}
