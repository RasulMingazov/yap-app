package app.yap.feature.auth.di

import app.yap.feature.auth.domain.entity.LoginProvider
import app.yap.feature.auth.domain.entity.LoginProviderId

/** Apple has no row on Android regardless of its enabled flag (R-017, AC-005). */
internal actual fun defaultLoginProviderConfig(): LoginProviderConfig = LoginProviderConfig(
    providers = listOf(
        LoginProvider(
            displayName = "Google",
            iconToken = "google",
            id = LoginProviderId.Google,
            isEnabled = true,
            isVisible = true,
        ),
        LoginProvider(
            displayName = "Apple",
            iconToken = "apple",
            id = LoginProviderId.Apple,
            isEnabled = false,
            isVisible = false,
        ),
        LoginProvider(
            displayName = "T-ID",
            iconToken = "tid",
            id = LoginProviderId.Tid,
            isEnabled = false,
            isVisible = true,
        ),
    ),
)
