package app.yap.feature.auth.presentation.login

import app.yap.feature.auth.domain.entity.LoginProvider
import app.yap.feature.auth.domain.entity.StubLoginProvider

internal object StubLoginDataState {

    fun stubLoginDataState(
        isLoading: Boolean = false,
        providers: List<LoginProvider> = StubLoginProvider.stubIosProviders(),
    ) = LoginDataState(
        isLoading = isLoading,
        providers = providers,
    )
}
