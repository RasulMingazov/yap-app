package app.yap.feature.auth.presentation.auth

internal object AuthStubs {

    fun stubDataState(
        isProviderSelectionPresented: Boolean = false,
    ) = AuthModel.DataState(isProviderSelectionPresented = isProviderSelectionPresented)

    fun stubUiState(
        isProviderSelectionPresented: Boolean = false,
    ) = AuthComponent.UiState(isProviderSelectionPresented = isProviderSelectionPresented)
}
