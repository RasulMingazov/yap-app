package app.yap.feature.auth.presentation.selectprovider

import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.presentation.common.AuthProviderUiMapper

internal class SelectAuthProviderUiStateMapper(private val authProviderUiMapper: AuthProviderUiMapper) {

    operator fun invoke(
        dataState: SelectAuthProviderViewModel.DataState,
    ): SelectAuthProviderViewModel.UiState = SelectAuthProviderViewModel.UiState(
        providers = dataState.providers
            .filter(AuthProvider::isVisible)
            .map(::toRow),
    )

    private fun toRow(provider: AuthProvider): SelectAuthProviderViewModel.UiState.Provider =
        SelectAuthProviderViewModel.UiState.Provider(
            provider = provider,
            ui = authProviderUiMapper(provider.type),
        )
}
