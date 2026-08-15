package app.yap.feature.auth.presentation.login

import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_failed
import app.yap.feature.auth.generated.resources.login_provider_soon
import app.yap.feature.auth.presentation.common.AuthProviderUiMapper

internal class LoginNewsMapper(private val authProviderUiMapper: AuthProviderUiMapper) {

    operator fun invoke(outcome: LoginOutcome, provider: AuthProvider): LoginViewModel.News? = when (outcome) {
        is LoginOutcome.Cancelled, is LoginOutcome.Success -> null

        is LoginOutcome.Unavailable -> LoginViewModel.News.ShowMessage(
            message = Res.string.login_provider_soon,
            argument = authProviderUiMapper(provider.type).labelRes,
        )

        is LoginOutcome.Failed -> LoginViewModel.News.ShowMessage(Res.string.login_failed)
    }
}
