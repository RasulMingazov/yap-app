package app.yap.feature.auth.di

import app.yap.feature.auth.domain.entity.LoginProvider
import app.yap.feature.auth.domain.entity.LoginProviderId

/** All three providers have a row on iOS; only Google is enabled (R-016, R-017, AC-006). */
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
            isVisible = true,
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
