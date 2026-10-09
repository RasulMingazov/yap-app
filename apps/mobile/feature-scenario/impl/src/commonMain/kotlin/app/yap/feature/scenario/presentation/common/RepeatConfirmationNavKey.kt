package app.yap.feature.scenario.presentation.common

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal data class RepeatConfirmationNavKey(
    val scenarioId: String,
    val title: String,
) : NavKey

internal object ScenarioResultKeys {

    const val REPEAT_CONFIRMATION = "scenario.repeat.confirmation"
}
