package app.yap.feature.auth.di

import android.content.Context
import app.yap.core.common.platform.ActivityProvider
import app.yap.core.common.platform.MotionPreferences
import app.yap.core.common.platform.Platform
import app.yap.core.network.NetworkClient
import kotlin.test.Test
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
                Context::class,
                MotionPreferences::class,
                NetworkClient::class,
                Platform::class,
            ),
        )
    }
}
