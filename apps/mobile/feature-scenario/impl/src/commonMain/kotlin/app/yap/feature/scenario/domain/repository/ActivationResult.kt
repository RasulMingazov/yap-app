package app.yap.feature.scenario.domain.repository

import app.yap.core.network.ApiError

internal sealed interface ActivationResult {

    data object Opened : ActivationResult

    data object AccessRequired : ActivationResult

    data object SlotLimitReached : ActivationResult

    data class Failed(val error: ApiError) : ActivationResult
}
