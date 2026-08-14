package app.yap.feature.auth.presentation.selectprovider

import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.ic_provider_apple
import app.yap.feature.auth.generated.resources.ic_provider_google
import app.yap.feature.auth.generated.resources.ic_provider_t_id
import app.yap.feature.auth.generated.resources.login_provider_apple
import app.yap.feature.auth.generated.resources.login_provider_google
import app.yap.feature.auth.generated.resources.login_provider_t_id
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SelectAuthProviderUiStateMapperTest {

    @Test
    fun `GIVEN a provider this device does not offer WHEN the state is mapped THEN it contributes no row`() {
        val uiState = SelectAuthProviderUiStateMapper(
            SelectAuthProviderViewModel.DataState(
                providers = listOf(GOOGLE, AuthProvider.Apple(isEnabled = false, isVisible = false)),
            ),
        )

        assertEquals(expected = listOf(GOOGLE), actual = uiState.providers.map { row -> row.provider })
    }

    @Test
    fun `GIVEN the roster WHEN the state is mapped THEN each row keeps the order it arrived in`() {
        val uiState = SelectAuthProviderUiStateMapper(
            SelectAuthProviderViewModel.DataState(providers = listOf(T_ID, GOOGLE, APPLE)),
        )

        assertEquals(expected = listOf(T_ID, GOOGLE, APPLE), actual = uiState.providers.map { row -> row.provider })
    }

    @Test
    fun `GIVEN a provider that may not be chosen WHEN the state is mapped THEN the row carries that`() {
        val uiState = SelectAuthProviderUiStateMapper(
            SelectAuthProviderViewModel.DataState(providers = listOf(GOOGLE, T_ID)),
        )

        assertEquals(expected = listOf(true, false), actual = uiState.providers.map { row -> row.isEnabled })
    }

    @Test
    fun `GIVEN every provider WHEN the state is mapped THEN each row carries its own name and mark`() {
        val uiState = SelectAuthProviderUiStateMapper(
            SelectAuthProviderViewModel.DataState(providers = listOf(GOOGLE, APPLE, T_ID)),
        )

        assertEquals(
            expected = listOf(
                Res.string.login_provider_google,
                Res.string.login_provider_apple,
                Res.string.login_provider_t_id,
            ),
            actual = uiState.providers.map { row -> row.labelRes },
        )
        assertEquals(
            expected = listOf(
                Res.drawable.ic_provider_google,
                Res.drawable.ic_provider_apple,
                Res.drawable.ic_provider_t_id,
            ),
            actual = uiState.providers.map { row -> row.iconRes },
        )
    }

    @Test
    fun `GIVEN every provider WHEN the state is mapped THEN only the theme-drawn mark is tinted`() {
        val uiState = SelectAuthProviderUiStateMapper(
            SelectAuthProviderViewModel.DataState(providers = listOf(GOOGLE, APPLE, T_ID)),
        )

        assertEquals(
            expected = listOf(false, true, false),
            actual = uiState.providers.map { row -> row.isMonochrome },
        )
    }

    private companion object {
        val APPLE: AuthProvider = AuthProvider.Apple(isEnabled = false, isVisible = true)
        val GOOGLE: AuthProvider = AuthProvider.Google(isEnabled = true, isVisible = true)
        val T_ID: AuthProvider = AuthProvider.TId(isEnabled = false, isVisible = true)
    }
}
