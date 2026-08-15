package app.yap.feature.auth.presentation.common

import app.yap.feature.auth.api.entity.AuthProviderType
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.ic_provider_apple
import app.yap.feature.auth.generated.resources.ic_provider_google
import app.yap.feature.auth.generated.resources.ic_provider_t_id
import app.yap.feature.auth.generated.resources.login_provider_apple
import app.yap.feature.auth.generated.resources.login_provider_google
import app.yap.feature.auth.generated.resources.login_provider_t_id

internal class AuthProviderUiMapper {

    operator fun invoke(type: AuthProviderType): AuthProviderUi = when (type) {
        AuthProviderType.APPLE -> AuthProviderUi(
            iconRes = Res.drawable.ic_provider_apple,
            isMonochrome = true,
            labelRes = Res.string.login_provider_apple,
            testTag = "login_provider_apple",
        )

        AuthProviderType.GOOGLE -> AuthProviderUi(
            iconRes = Res.drawable.ic_provider_google,
            isMonochrome = false,
            labelRes = Res.string.login_provider_google,
            testTag = "login_provider_google",
        )

        AuthProviderType.T_ID -> AuthProviderUi(
            iconRes = Res.drawable.ic_provider_t_id,
            isMonochrome = false,
            labelRes = Res.string.login_provider_t_id,
            testTag = "login_provider_t_id",
        )
    }
}
