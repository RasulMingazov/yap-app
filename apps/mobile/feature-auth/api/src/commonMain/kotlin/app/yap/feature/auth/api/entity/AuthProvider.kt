package app.yap.feature.auth.api.entity

data class AuthProvider(
    val type: AuthProviderType,
    val isEnabled: Boolean,
    val isVisible: Boolean,
)
