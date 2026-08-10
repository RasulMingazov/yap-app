package app.yap.feature.auth.domain.entity

internal object StubLoginProvider {

    const val APPLE_DISPLAY_NAME = "Apple"
    const val APPLE_ICON_TOKEN = "apple"
    const val GOOGLE_DISPLAY_NAME = "Google"
    const val GOOGLE_ICON_TOKEN = "google"
    const val TID_DISPLAY_NAME = "T-ID"
    const val TID_ICON_TOKEN = "tid"

    fun stubLoginProvider(
        displayName: String = GOOGLE_DISPLAY_NAME,
        iconToken: String = GOOGLE_ICON_TOKEN,
        id: LoginProviderId = LoginProviderId.Google,
        isEnabled: Boolean = true,
        isVisible: Boolean = true,
    ): LoginProvider = LoginProvider(
        displayName = displayName,
        iconToken = iconToken,
        id = id,
        isEnabled = isEnabled,
        isVisible = isVisible,
    )

    fun stubApple(
        isEnabled: Boolean = false,
        isVisible: Boolean = true,
    ): LoginProvider = stubLoginProvider(
        displayName = APPLE_DISPLAY_NAME,
        iconToken = APPLE_ICON_TOKEN,
        id = LoginProviderId.Apple,
        isEnabled = isEnabled,
        isVisible = isVisible,
    )

    fun stubGoogle(
        isEnabled: Boolean = true,
        isVisible: Boolean = true,
    ): LoginProvider = stubLoginProvider(
        displayName = GOOGLE_DISPLAY_NAME,
        iconToken = GOOGLE_ICON_TOKEN,
        id = LoginProviderId.Google,
        isEnabled = isEnabled,
        isVisible = isVisible,
    )

    fun stubTid(
        isEnabled: Boolean = false,
        isVisible: Boolean = true,
    ): LoginProvider = stubLoginProvider(
        displayName = TID_DISPLAY_NAME,
        iconToken = TID_ICON_TOKEN,
        id = LoginProviderId.Tid,
        isEnabled = isEnabled,
        isVisible = isVisible,
    )

    fun stubAndroidProviders(): List<LoginProvider> = listOf(
        stubGoogle(),
        stubApple(isVisible = false),
        stubTid(),
    )

    fun stubIosProviders(): List<LoginProvider> = listOf(
        stubGoogle(),
        stubApple(),
        stubTid(),
    )
}
