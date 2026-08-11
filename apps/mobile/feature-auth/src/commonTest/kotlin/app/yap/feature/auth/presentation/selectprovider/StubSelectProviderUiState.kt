package app.yap.feature.auth.presentation.selectprovider

import app.yap.feature.auth.domain.entity.LoginProviderId
import app.yap.feature.auth.domain.entity.StubLoginProvider
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.select_provider_empty
import app.yap.feature.auth.generated.resources.select_provider_title
import org.jetbrains.compose.resources.StringResource

internal object StubSelectProviderUiState {

    fun stubSelectProviderUiState(
        emptyMessage: StringResource? = null,
        isLoading: Boolean = false,
        providers: List<SelectProviderUiState.Provider> = emptyList(),
        title: StringResource = Res.string.select_provider_title,
    ) = SelectProviderUiState(
        emptyMessage = emptyMessage,
        isLoading = isLoading,
        providers = providers,
        title = title,
    )

    fun stubEmptyUiState(
        emptyMessage: StringResource = Res.string.select_provider_empty,
    ) = stubSelectProviderUiState(
        emptyMessage = emptyMessage,
        providers = emptyList(),
    )

    fun stubLoadingUiState() = stubSelectProviderUiState(isLoading = true)

    fun stubProviderRow(
        displayName: String,
        iconToken: String,
        id: LoginProviderId,
        isEnabled: Boolean,
        key: String,
    ) = SelectProviderUiState.Provider(
        displayName = displayName,
        iconToken = iconToken,
        id = id,
        isEnabled = isEnabled,
        key = key,
    )

    fun stubAppleRow(
        isEnabled: Boolean = false,
    ) = stubProviderRow(
        displayName = StubLoginProvider.APPLE_DISPLAY_NAME,
        iconToken = StubLoginProvider.APPLE_ICON_TOKEN,
        id = LoginProviderId.Apple,
        isEnabled = isEnabled,
        key = "apple",
    )

    fun stubGoogleRow(
        isEnabled: Boolean = true,
    ) = stubProviderRow(
        displayName = StubLoginProvider.GOOGLE_DISPLAY_NAME,
        iconToken = StubLoginProvider.GOOGLE_ICON_TOKEN,
        id = LoginProviderId.Google,
        isEnabled = isEnabled,
        key = "google",
    )

    fun stubTidRow(
        isEnabled: Boolean = false,
    ) = stubProviderRow(
        displayName = StubLoginProvider.TID_DISPLAY_NAME,
        iconToken = StubLoginProvider.TID_ICON_TOKEN,
        id = LoginProviderId.Tid,
        isEnabled = isEnabled,
        key = "tid",
    )
}
