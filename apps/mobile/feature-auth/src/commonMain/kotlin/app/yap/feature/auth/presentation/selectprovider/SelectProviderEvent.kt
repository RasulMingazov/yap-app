package app.yap.feature.auth.presentation.selectprovider

import app.yap.feature.auth.domain.entity.LoginProviderId

sealed interface SelectProviderEvent {

    data object DismissRequested : SelectProviderEvent

    data class ProviderClicked(val providerId: LoginProviderId) : SelectProviderEvent
}
