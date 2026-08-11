package app.yap.feature.auth.presentation.selectprovider

import app.yap.feature.auth.domain.entity.LoginProvider
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.select_provider_empty
import app.yap.feature.auth.generated.resources.select_provider_title

/**
 * Keeps the configured order verbatim, drops the rows the platform does not support, and falls back
 * to the empty-state message while the sheet keeps its title (R-011, R-013, AC-016, AC-040, AC-041).
 */
internal fun SelectProviderDataState.toUiState(): SelectProviderUiState {
    val rows = providers
        ?.filter { it.isVisible }
        ?.map { it.toRow() }

    return SelectProviderUiState(
        isLoading = rows == null,
        emptyMessage = Res.string.select_provider_empty.takeIf { rows != null && rows.isEmpty() },
        providers = rows ?: emptyList(),
        title = Res.string.select_provider_title,
    )
}

private fun LoginProvider.toRow(): SelectProviderUiState.Provider =
    SelectProviderUiState.Provider(
        displayName = displayName,
        iconToken = iconToken,
        id = id,
        isEnabled = isEnabled,
        key = id.id,
    )
