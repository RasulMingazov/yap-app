package app.yap.feature.auth.presentation.login

import org.jetbrains.compose.resources.StringResource

sealed interface LoginNews {

    data class ShowSnackbar(
        val formatArgs: List<String>,
        val message: StringResource,
    ) : LoginNews
}
