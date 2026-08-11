package app.yap.feature.auth.presentation.selectprovider

import app.yap.feature.auth.domain.entity.LoginProvider
import app.yap.feature.auth.domain.entity.StubLoginProvider

internal object StubSelectProviderDataState {

    fun stubSelectProviderDataState(
        providers: List<LoginProvider>? = StubLoginProvider.stubIosProviders(),
    ) = SelectProviderDataState(providers = providers)
}
