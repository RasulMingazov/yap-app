package app.yap.feature.auth.presentation.login

import org.jetbrains.compose.resources.StringResource

data class LoginUiState(
    val body: StringResource,
    val button: Button,
    val caption: StringResource,
    val hero: StringResource,
    val marquee: StringResource,
    val topics: List<StringResource>,
) {

    /** The action button is either labelled or loading; loading replaces the label (R-027, AC-044). */
    sealed interface Button {

        data class Label(val text: StringResource) : Button

        data object Loading : Button
    }
}
