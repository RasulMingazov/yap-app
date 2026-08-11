package app.yap.feature.auth.presentation.selectprovider

import app.yap.feature.auth.domain.entity.LoginProviderId
import org.jetbrains.compose.resources.StringResource

data class SelectProviderUiState(
    val emptyMessage: StringResource?,
    val isLoading: Boolean,
    val providers: List<Provider>,
    val title: StringResource,
) {

    /** One row, carrying every repeatable fact the sheet needs to render it (R-069, AC-041). */
    data class Provider(
        val displayName: String,
        val iconToken: String,
        val id: LoginProviderId,
        val isEnabled: Boolean,
        val key: String,
    )
}
