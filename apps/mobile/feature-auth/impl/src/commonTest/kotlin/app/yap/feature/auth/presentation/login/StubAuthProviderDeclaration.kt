package app.yap.feature.auth.presentation.login

import app.yap.core.common.platform.Platform
import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_provider_google
import org.jetbrains.compose.resources.StringResource

internal fun stubAuthProviderDeclaration(
    provider: AuthProvider,
    isUsable: Boolean = false,
    labelRes: StringResource = Res.string.login_provider_google,
    position: Int = 0,
    shownOn: Set<Platform> = setOf(Platform.ANDROID, Platform.IOS),
): AuthProviderDeclaration = AuthProviderDeclaration(
    isUsable = isUsable,
    labelRes = labelRes,
    position = position,
    provider = provider,
    shownOn = shownOn,
)
