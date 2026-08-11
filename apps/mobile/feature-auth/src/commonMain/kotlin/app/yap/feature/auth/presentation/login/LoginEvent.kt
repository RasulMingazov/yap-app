package app.yap.feature.auth.presentation.login

import app.yap.feature.auth.domain.entity.LoginProviderId

sealed interface LoginEvent {

    data object LoginClicked : LoginEvent

    data class ProviderSelected(val providerId: LoginProviderId) : LoginEvent
}
