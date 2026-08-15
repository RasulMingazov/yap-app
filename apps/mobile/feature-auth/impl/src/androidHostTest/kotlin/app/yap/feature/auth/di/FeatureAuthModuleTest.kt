package app.yap.feature.auth.di

import android.content.Context
import app.yap.core.common.navigation.Navigator
import app.yap.core.common.platform.ActivityProvider
import app.yap.core.common.platform.MotionPreferences
import app.yap.core.common.platform.Platform
import app.yap.core.network.ApiClient
import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.usecase.LoginUseCase
import app.yap.feature.auth.api.usecase.ObserveAuthProvidersUseCase
import app.yap.feature.auth.domain.provider.ProviderLogin
import app.yap.feature.auth.domain.repository.GoogleAuthRepository
import app.yap.feature.auth.domain.repository.StubGoogleAuthRepository
import app.yap.feature.auth.domain.usecase.DefaultObserveAuthProvidersUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.verify.verify

internal class FeatureAuthModuleTest {

    @Test
    fun `GIVEN the feature module WHEN it is verified THEN every binding resolves`() {
        featureAuthModule(
            googleServerClientId = "web-client.apps.googleusercontent.com",
            privacyUrl = null,
            termsUrl = null,
            googleAndroidClientId = "android-client.apps.googleusercontent.com",
            googleRedirectUri = "app.yap.oauth:/oauth2redirect",
        ).verify(
            extraTypes = listOf(
                ActivityProvider::class,
                ApiClient::class,
                Context::class,
                MotionPreferences::class,
                Navigator::class,
                Platform::class,
            ),
        )
    }

    @Test
    fun `GIVEN the login entry point WHEN it is resolved THEN its own graph is complete`() {
        val koin = graph()

        try {
            koin.get<LoginUseCase>()
            koin.get<ObserveAuthProvidersUseCase>()
        } finally {
            koin.close()
        }
    }

    @Test
    fun `GIVEN the roster WHEN a provider may be chosen THEN the graph holds one login path for it`() = runTest {
        val koin = graph()
        val registered = koin.getAll<ProviderLogin>().map(ProviderLogin::type)

        try {
            assertEquals(
                expected = registered.distinct(),
                actual = registered,
                message = "two ProviderLogin bindings claim the same provider, so one is silently unreachable",
            )
            Platform.entries.forEach { platform ->
                val selectable = DefaultObserveAuthProvidersUseCase(platform = platform)()
                    .first()
                    .filter(AuthProvider::isEnabled)
                    .map(AuthProvider::type)

                assertEquals(
                    expected = selectable,
                    actual = selectable.filter { provider -> provider in registered },
                    message = "$platform offers a provider with no registered ProviderLogin",
                )
            }
        } finally {
            koin.close()
        }
    }

    private fun graph(): Koin = koinApplication {
        modules(
            featureAuthModule(
                googleServerClientId = "web-client.apps.googleusercontent.com",
                privacyUrl = null,
                termsUrl = null,
            ),
            module {
                single<GoogleAuthRepository> { StubGoogleAuthRepository() }
                single<Platform> { Platform.ANDROID }
            },
        )
    }.koin
}
