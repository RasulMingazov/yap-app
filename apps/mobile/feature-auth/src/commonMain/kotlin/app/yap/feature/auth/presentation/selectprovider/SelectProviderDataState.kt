package app.yap.feature.auth.presentation.selectprovider

import app.yap.feature.auth.domain.entity.LoginProvider

internal data class SelectProviderDataState(
    val providers: List<LoginProvider>? = null,
)
