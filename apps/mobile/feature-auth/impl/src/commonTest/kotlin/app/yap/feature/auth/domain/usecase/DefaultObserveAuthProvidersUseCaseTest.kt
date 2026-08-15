package app.yap.feature.auth.domain.usecase

import app.yap.core.common.platform.Platform
import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.entity.AuthProviderType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

internal class DefaultObserveAuthProvidersUseCaseTest {

    @Test
    fun `GIVEN any platform WHEN the roster is observed THEN it names Google then Apple then T-ID`() = runTest {
        val providers = roster(platform = Platform.IOS)

        assertEquals(
            expected = listOf(
                AuthProviderType.GOOGLE,
                AuthProviderType.APPLE,
                AuthProviderType.T_ID,
            ),
            actual = providers.map(AuthProvider::type),
        )
    }

    @Test
    fun `GIVEN iOS WHEN the roster is observed THEN Apple is offered`() = runTest {
        val providers = roster(platform = Platform.IOS)

        assertEquals(
            expected = listOf(true, true, true),
            actual = providers.map(AuthProvider::isVisible),
        )
    }

    @Test
    fun `GIVEN Android WHEN the roster is observed THEN Apple is not offered`() = runTest {
        val providers = roster(platform = Platform.ANDROID)

        assertEquals(
            expected = listOf(true, false, true),
            actual = providers.map(AuthProvider::isVisible),
        )
    }

    @Test
    fun `GIVEN any platform WHEN the roster is observed THEN only Google may be chosen`() = runTest {
        assertEquals(
            expected = listOf(AuthProviderType.GOOGLE),
            actual = roster(platform = Platform.ANDROID)
                .filter(AuthProvider::isEnabled)
                .map(AuthProvider::type),
        )
        assertEquals(
            expected = listOf(AuthProviderType.GOOGLE),
            actual = roster(platform = Platform.IOS)
                .filter(AuthProvider::isEnabled)
                .map(AuthProvider::type),
        )
    }

    private suspend fun roster(platform: Platform): List<AuthProvider> =
        DefaultObserveAuthProvidersUseCase(platform = platform)().first()
}
