package app.yap.feature.auth.presentation.selectprovider

import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.entity.AuthProviderType
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.ic_provider_apple
import app.yap.feature.auth.generated.resources.ic_provider_google
import app.yap.feature.auth.generated.resources.ic_provider_t_id
import app.yap.feature.auth.generated.resources.login_provider_apple
import app.yap.feature.auth.generated.resources.login_provider_google
import app.yap.feature.auth.generated.resources.login_provider_t_id
import app.yap.feature.auth.presentation.common.AuthProviderUiMapper
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SelectAuthProviderUiStateMapperTest {

    private val mapper = SelectAuthProviderUiStateMapper(authProviderUiMapper = AuthProviderUiMapper())

    @Test
    fun `GIVEN a provider this device does not offer WHEN the state is mapped THEN it contributes no row`() {
        val uiState = mapper(
            SelectAuthProviderViewModel.DataState(
                providers = listOf(GOOGLE, AuthProvider(type = AuthProviderType.APPLE, isEnabled = false, isVisible = false)),
            ),
        )

        assertEquals(expected = listOf(GOOGLE), actual = uiState.providers.map { row -> row.provider })
    }

    @Test
    fun `GIVEN the roster WHEN the state is mapped THEN each row keeps the order it arrived in`() {
        val uiState = mapper(SelectAuthProviderViewModel.DataState(providers = listOf(T_ID, GOOGLE, APPLE)))

        assertEquals(expected = listOf(T_ID, GOOGLE, APPLE), actual = uiState.providers.map { row -> row.provider })
    }

    @Test
    fun `GIVEN a provider that may not be chosen WHEN the state is mapped THEN the row carries that`() {
        val uiState = mapper(SelectAuthProviderViewModel.DataState(providers = listOf(GOOGLE, T_ID)))

        assertEquals(expected = listOf(true, false), actual = uiState.providers.map { row -> row.provider.isEnabled })
    }

    @Test
    fun `GIVEN every provider WHEN the state is mapped THEN each row carries its own name and mark`() {
        val uiState = mapper(SelectAuthProviderViewModel.DataState(providers = listOf(GOOGLE, APPLE, T_ID)))

        assertEquals(
            expected = listOf(
                Res.string.login_provider_google,
                Res.string.login_provider_apple,
                Res.string.login_provider_t_id,
            ),
            actual = uiState.providers.map { row -> row.ui.labelRes },
        )
        assertEquals(
            expected = listOf(
                Res.drawable.ic_provider_google,
                Res.drawable.ic_provider_apple,
                Res.drawable.ic_provider_t_id,
            ),
            actual = uiState.providers.map { row -> row.ui.iconRes },
        )
    }

    @Test
    fun `GIVEN every provider WHEN the state is mapped THEN only the theme-drawn mark is tinted`() {
        val uiState = mapper(SelectAuthProviderViewModel.DataState(providers = listOf(GOOGLE, APPLE, T_ID)))

        assertEquals(
            expected = listOf(false, true, false),
            actual = uiState.providers.map { row -> row.ui.isMonochrome },
        )
    }

    private companion object {
        val APPLE: AuthProvider = AuthProvider(type = AuthProviderType.APPLE, isEnabled = false, isVisible = true)
        val GOOGLE: AuthProvider = AuthProvider(type = AuthProviderType.GOOGLE, isEnabled = true, isVisible = true)
        val T_ID: AuthProvider = AuthProvider(type = AuthProviderType.T_ID, isEnabled = false, isVisible = true)
    }
}
