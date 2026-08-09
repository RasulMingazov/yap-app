package app.yap.feature.auth.domain.entity

internal data class LoginProvider(
    val displayName: String,
    val iconToken: String,
    val id: LoginProviderId,
    val isEnabled: Boolean,
    val isVisible: Boolean,
)
