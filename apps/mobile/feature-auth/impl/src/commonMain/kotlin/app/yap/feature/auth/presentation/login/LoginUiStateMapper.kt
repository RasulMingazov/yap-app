package app.yap.feature.auth.presentation.login

import app.yap.core.common.platform.Platform
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_topic_meeting_people
import app.yap.feature.auth.generated.resources.login_topic_rejections
import app.yap.feature.auth.generated.resources.login_topic_small_talk
import org.jetbrains.compose.resources.StringResource

internal object LoginUiStateMapper {

    private val TOPICS: List<StringResource> = listOf(
        Res.string.login_topic_small_talk,
        Res.string.login_topic_rejections,
        Res.string.login_topic_meeting_people,
    )

    operator fun invoke(
        dataState: LoginViewModel.DataState,
        isMotionReduced: Boolean,
        platform: Platform,
        privacyUrl: String?,
        termsUrl: String?,
        declarations: List<AuthProviderDeclaration> = AuthProviderCatalog.DECLARATIONS,
    ): LoginViewModel.UiState = LoginViewModel.UiState(
        isProviderSheetVisible = dataState.isProviderSheetVisible,
        isMotionReduced = isMotionReduced,
        isLoggingIn = dataState.isLoggingIn,
        providers = providersFor(declarations = declarations, platform = platform),
        privacyUrl = privacyUrl,
        termsUrl = termsUrl,
        topics = TOPICS,
    )

    private fun providersFor(
        declarations: List<AuthProviderDeclaration>,
        platform: Platform,
    ): List<LoginViewModel.UiState.Provider> =
        declarations.fold(emptyList()) { rows, declaration ->
            if (platform in declaration.shownOn) rows + declaration.toRow() else rows
        }

    private fun AuthProviderDeclaration.toRow(): LoginViewModel.UiState.Provider =
        LoginViewModel.UiState.Provider(
            isAvailable = isUsable,
            labelRes = labelRes,
            provider = provider,
        )
}
