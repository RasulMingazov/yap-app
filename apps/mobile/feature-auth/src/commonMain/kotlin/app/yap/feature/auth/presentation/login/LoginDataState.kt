package app.yap.feature.auth.presentation.login

import app.yap.feature.auth.domain.entity.LoginProvider

internal data class LoginDataState(
    val isLoading: Boolean = false,
    val providers: List<LoginProvider> = emptyList(),
)
