package app.yap.feature.auth.api.entity

sealed interface AuthProvider {

    val isEnabled: Boolean

    val isVisible: Boolean

    data class Apple(
        override val isEnabled: Boolean,
        override val isVisible: Boolean,
    ) : AuthProvider

    data class Google(
        override val isEnabled: Boolean,
        override val isVisible: Boolean,
    ) : AuthProvider

    data class TId(
        override val isEnabled: Boolean,
        override val isVisible: Boolean,
    ) : AuthProvider
}
