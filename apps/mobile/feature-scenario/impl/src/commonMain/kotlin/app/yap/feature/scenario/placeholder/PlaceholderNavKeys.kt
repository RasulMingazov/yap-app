package app.yap.feature.scenario.placeholder

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal data object SessionComingSoonNavKey : NavKey

@Serializable
internal data class PlaceholderPaywallNavKey(
    val scenarioId: String?,
    val origin: String,
) : NavKey
