package app.yap.feature.auth.presentation.selectprovider

import app.yap.feature.auth.domain.entity.LoginProvider
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.select_provider_empty
import app.yap.feature.auth.generated.resources.select_provider_title

/**
 * Keeps the configured order verbatim, drops the rows the platform does not support, and falls back
 * to the empty-state message while the sheet keeps its title (R-011, R-013, AC-016, AC-040, AC-041).
 */
internal interface SelectProviderUiStateMapper {

    operator fun invoke(dataState: SelectProviderModel.DataState): SelectProviderComponent.UiState
}

internal class DefaultSelectProviderUiStateMapper : SelectProviderUiStateMapper {

    override fun invoke(dataState: SelectProviderModel.DataState): SelectProviderComponent.UiState {
        val rows = dataState.providers.filter(LoginProvider::isVisible).map(LoginProvider::toRow)

        return SelectProviderComponent.UiState(
            emptyMessage = Res.string.select_provider_empty.takeIf { rows.isEmpty() },
            providers = rows,
            title = Res.string.select_provider_title,
        )
    }
}

private fun LoginProvider.toRow(): SelectProviderComponent.UiState.Provider =
    SelectProviderComponent.UiState.Provider(
        displayName = displayName,
        iconToken = iconToken,
        id = id,
        isEnabled = isEnabled,
        key = id.id,
    )
