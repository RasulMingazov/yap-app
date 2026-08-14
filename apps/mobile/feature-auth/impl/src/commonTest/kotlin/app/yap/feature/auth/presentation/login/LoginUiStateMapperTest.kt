package app.yap.feature.auth.presentation.login

import app.yap.core.common.platform.Platform
import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.generated.resources.Res
import app.yap.feature.auth.generated.resources.login_provider_apple
import app.yap.feature.auth.generated.resources.login_provider_google
import app.yap.feature.auth.generated.resources.login_provider_t_id
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class LoginUiStateMapperTest {

    @Test
    fun `GIVEN Android WHEN the state is mapped THEN Google and T-ID are listed and Apple is absent`() {
        val uiState = map(platform = Platform.ANDROID)

        assertEquals(
            expected = listOf(AuthProvider.GOOGLE, AuthProvider.T_ID),
            actual = uiState.providers.map { provider -> provider.provider },
        )
    }

    @Test
    fun `GIVEN Android and an attempt in progress WHEN the state is mapped THEN Apple is still absent`() {
        val uiState = map(
            dataState = LoginViewModel.DataState(isLoggingIn = true),
            platform = Platform.ANDROID,
        )

        assertEquals(
            expected = listOf(AuthProvider.GOOGLE, AuthProvider.T_ID),
            actual = uiState.providers.map { provider -> provider.provider },
        )
    }

    @Test
    fun `GIVEN iOS WHEN the state is mapped THEN all three providers are listed`() {
        val uiState = map(platform = Platform.IOS)

        assertEquals(
            expected = listOf(AuthProvider.GOOGLE, AuthProvider.APPLE, AuthProvider.T_ID),
            actual = uiState.providers.map { provider -> provider.provider },
        )
    }

    @Test
    fun `GIVEN iOS WHEN the state is mapped THEN only Google is available`() {
        val uiState = map(platform = Platform.IOS)

        assertEquals(
            expected = listOf(AuthProvider.GOOGLE),
            actual = uiState.providers.filter { provider -> provider.isAvailable }.map { provider -> provider.provider },
        )
    }

    @Test
    fun `GIVEN each platform WHEN the state is mapped THEN every provider carries its own name`() {
        val uiState = map(platform = Platform.IOS)

        assertEquals(
            expected = listOf(
                Res.string.login_provider_google,
                Res.string.login_provider_apple,
                Res.string.login_provider_t_id,
            ),
            actual = uiState.providers.map { provider -> provider.labelRes },
        )
    }

    @Test
    fun `GIVEN an attempt in progress WHEN the state is mapped THEN progress is shown and every row stays`() {
        val uiState = map(dataState = LoginViewModel.DataState(isLoggingIn = true), platform = Platform.ANDROID)

        assertEquals(expected = true, actual = uiState.isLoggingIn)
        assertEquals(expected = 2, actual = uiState.providers.size)
    }

    @Test
    fun `GIVEN the system asks for reduced motion WHEN the state is mapped THEN animation is off`() {
        val uiState = map(isMotionReduced = true, platform = Platform.ANDROID)

        assertEquals(expected = true, actual = uiState.isMotionReduced)
    }

    @Test
    fun `GIVEN the system allows motion WHEN the state is mapped THEN animation is on`() {
        val uiState = map(isMotionReduced = false, platform = Platform.ANDROID)

        assertEquals(expected = false, actual = uiState.isMotionReduced)
    }

    @Test
    fun `GIVEN both legal destinations are configured WHEN the state is mapped THEN both are carried`() {
        val uiState = map(platform = Platform.ANDROID, privacyUrl = PRIVACY_URL, termsUrl = TERMS_URL)

        assertEquals(expected = TERMS_URL, actual = uiState.termsUrl)
        assertEquals(expected = PRIVACY_URL, actual = uiState.privacyUrl)
    }

    @Test
    fun `GIVEN an unset terms destination WHEN the state is mapped THEN the link has nowhere to go`() {
        val uiState = map(platform = Platform.ANDROID, privacyUrl = PRIVACY_URL, termsUrl = null)

        assertNull(actual = uiState.termsUrl)
        assertEquals(expected = PRIVACY_URL, actual = uiState.privacyUrl)
    }

    @Test
    fun `GIVEN an unset privacy destination WHEN the state is mapped THEN the link has nowhere to go`() {
        val uiState = map(platform = Platform.ANDROID, privacyUrl = null, termsUrl = TERMS_URL)

        assertNull(actual = uiState.privacyUrl)
        assertEquals(expected = TERMS_URL, actual = uiState.termsUrl)
    }

    @Test
    fun `GIVEN the sheet has been opened WHEN the state is mapped THEN it is visible`() {
        val uiState = map(
            dataState = LoginViewModel.DataState(isProviderSheetVisible = true),
            platform = Platform.ANDROID,
        )

        assertEquals(expected = true, actual = uiState.isProviderSheetVisible)
    }

    @Test
    fun `GIVEN a provider declared hidden WHEN the state is mapped THEN it contributes no row on either platform`() {
        val declarations = listOf(stubAuthProviderDeclaration(provider = AuthProvider.T_ID, shownOn = emptySet()))

        assertEquals(
            expected = emptyList(),
            actual = map(declarations = declarations, platform = Platform.ANDROID).providers,
        )
        assertEquals(
            expected = emptyList(),
            actual = map(declarations = declarations, platform = Platform.IOS).providers,
        )
    }

    @Test
    fun `GIVEN a provider declared shown WHEN the state is mapped THEN it contributes exactly one row`() {
        val declarations = listOf(stubAuthProviderDeclaration(provider = AuthProvider.T_ID, shownOn = setOf(Platform.ANDROID)))

        val uiState = map(declarations = declarations, platform = Platform.ANDROID)

        assertEquals(expected = listOf(AuthProvider.T_ID), actual = uiState.providers.map { row -> row.provider })
    }

    @Test
    fun `GIVEN a provider declared usable but hidden WHEN the state is mapped THEN it contributes no row at all`() {
        val declarations = listOf(
            stubAuthProviderDeclaration(provider = AuthProvider.T_ID, isUsable = true, shownOn = emptySet()),
        )

        val uiState = map(declarations = declarations, platform = Platform.ANDROID)

        assertEquals(expected = emptyList(), actual = uiState.providers)
    }

    @Test
    fun `GIVEN a provider declared shown but not usable WHEN the state is mapped THEN its row reports so`() {
        val declarations = listOf(
            stubAuthProviderDeclaration(provider = AuthProvider.T_ID, isUsable = false, shownOn = setOf(Platform.ANDROID)),
        )

        val uiState = map(declarations = declarations, platform = Platform.ANDROID)

        assertEquals(expected = listOf(false), actual = uiState.providers.map { row -> row.isAvailable })
    }

    @Test
    fun `GIVEN declarations in a chosen order WHEN the state is mapped THEN the rows follow it`() {
        val declarations = listOf(
            stubAuthProviderDeclaration(provider = AuthProvider.T_ID),
            stubAuthProviderDeclaration(provider = AuthProvider.GOOGLE),
            stubAuthProviderDeclaration(provider = AuthProvider.APPLE),
        )

        val uiState = map(declarations = declarations, platform = Platform.ANDROID)

        assertEquals(
            expected = listOf(AuthProvider.T_ID, AuthProvider.GOOGLE, AuthProvider.APPLE),
            actual = uiState.providers.map { row -> row.provider },
        )
    }

    private fun map(
        platform: Platform,
        dataState: LoginViewModel.DataState = LoginViewModel.DataState(),
        declarations: List<AuthProviderDeclaration> = AuthProviderCatalog.DECLARATIONS,
        isMotionReduced: Boolean = false,
        privacyUrl: String? = PRIVACY_URL,
        termsUrl: String? = TERMS_URL,
    ): LoginViewModel.UiState = LoginUiStateMapper(
        dataState = dataState,
        isMotionReduced = isMotionReduced,
        platform = platform,
        privacyUrl = privacyUrl,
        termsUrl = termsUrl,
        declarations = declarations,
    )

    private companion object {
        const val PRIVACY_URL = "https://yap.app/privacy"
        const val TERMS_URL = "https://yap.app/terms"
    }
}
