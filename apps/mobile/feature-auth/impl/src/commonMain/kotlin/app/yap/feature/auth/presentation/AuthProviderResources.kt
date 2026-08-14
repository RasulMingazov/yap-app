package app.yap.feature.auth.presentation

import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.ic_provider_apple
import app.yap.feature.auth.generated.resources.ic_provider_google
import app.yap.feature.auth.generated.resources.ic_provider_t_id
import app.yap.feature.auth.generated.resources.login_provider_apple
import app.yap.feature.auth.generated.resources.login_provider_google
import app.yap.feature.auth.generated.resources.login_provider_t_id
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

internal object AuthProviderResources {

    fun labelOf(provider: AuthProvider): StringResource = presentationOf(provider).labelRes

    fun markOf(provider: AuthProvider): ProviderMark = presentationOf(provider).mark

    fun testTagOf(provider: AuthProvider): String = presentationOf(provider).testTag

    private fun presentationOf(provider: AuthProvider): Presentation = when (provider) {
        is AuthProvider.Apple -> Presentation(
            labelRes = Res.string.login_provider_apple,
            mark = ProviderMark(iconRes = Res.drawable.ic_provider_apple, isMonochrome = true),
            testTag = "login_provider_apple",
        )

        is AuthProvider.Google -> Presentation(
            labelRes = Res.string.login_provider_google,
            mark = ProviderMark(iconRes = Res.drawable.ic_provider_google, isMonochrome = false),
            testTag = "login_provider_google",
        )

        is AuthProvider.TId -> Presentation(
            labelRes = Res.string.login_provider_t_id,
            mark = ProviderMark(iconRes = Res.drawable.ic_provider_t_id, isMonochrome = false),
            testTag = "login_provider_t_id",
        )
    }

    data class ProviderMark(
        val iconRes: DrawableResource,
        val isMonochrome: Boolean,
    )

    private data class Presentation(
        val labelRes: StringResource,
        val mark: ProviderMark,
        val testTag: String,
    )
}
