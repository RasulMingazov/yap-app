package app.yap.feature.auth.presentation.selectprovider

import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.presentation.AuthProviderResources

internal object SelectAuthProviderUiStateMapper {

    operator fun invoke(
        dataState: SelectAuthProviderViewModel.DataState,
    ): SelectAuthProviderViewModel.UiState = SelectAuthProviderViewModel.UiState(
        providers = dataState.providers
            .filter(AuthProvider::isVisible)
            .map(::toRow),
    )

    private fun toRow(provider: AuthProvider): SelectAuthProviderViewModel.UiState.Provider {
        val mark = AuthProviderResources.markOf(provider)

        return SelectAuthProviderViewModel.UiState.Provider(
            iconRes = mark.iconRes,
            isEnabled = provider.isEnabled,
            isMonochrome = mark.isMonochrome,
            labelRes = AuthProviderResources.labelOf(provider),
            provider = provider,
        )
    }
}
