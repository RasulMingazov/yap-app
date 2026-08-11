package app.yap.feature.auth.presentation.selectprovider

import app.yap.feature.auth.domain.entity.LoginProviderId

sealed interface SelectProviderOutput {

    data object Dismissed : SelectProviderOutput

    data class ProviderSelected(val providerId: LoginProviderId) : SelectProviderOutput
}
