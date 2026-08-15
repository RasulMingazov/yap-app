package app.yap.feature.auth.presentation.common

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

internal data class AuthProviderUi(
    val iconRes: DrawableResource,
    val isMonochrome: Boolean,
    val labelRes: StringResource,
    val testTag: String,
)
