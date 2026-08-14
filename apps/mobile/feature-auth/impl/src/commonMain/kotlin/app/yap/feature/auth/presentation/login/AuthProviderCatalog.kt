package app.yap.feature.auth.presentation.login

import app.yap.core.common.platform.Platform
import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_provider_apple
import app.yap.feature.auth.generated.resources.login_provider_google
import app.yap.feature.auth.generated.resources.login_provider_t_id
import org.jetbrains.compose.resources.StringResource

internal data class AuthProviderDeclaration(
    val isUsable: Boolean,
    val labelRes: StringResource,
    val position: Int,
    val provider: AuthProvider,
    val shownOn: Set<Platform>,
)

internal object AuthProviderCatalog {

    val DECLARATIONS: List<AuthProviderDeclaration> = AuthProvider.entries
        .map(::declarationOf)
        .sortedBy(AuthProviderDeclaration::position)

    private fun declarationOf(provider: AuthProvider): AuthProviderDeclaration = when (provider) {
        AuthProvider.APPLE -> AuthProviderDeclaration(
            isUsable = false,
            labelRes = Res.string.login_provider_apple,
            position = 1,
            provider = AuthProvider.APPLE,
            shownOn = setOf(Platform.IOS),
        )

        AuthProvider.GOOGLE -> AuthProviderDeclaration(
            isUsable = true,
            labelRes = Res.string.login_provider_google,
            position = 0,
            provider = AuthProvider.GOOGLE,
            shownOn = setOf(Platform.ANDROID, Platform.IOS),
        )

        AuthProvider.T_ID -> AuthProviderDeclaration(
            isUsable = false,
            labelRes = Res.string.login_provider_t_id,
            position = 2,
            provider = AuthProvider.T_ID,
            shownOn = setOf(Platform.ANDROID, Platform.IOS),
        )
    }
}
