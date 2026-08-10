package app.yap.feature.auth.di

import app.yap.feature.auth.domain.entity.StubLoginProvider
import kotlin.test.Test
import kotlin.test.assertEquals

// Detekt's default test-source excludes do not cover the `androidHostTest` source set yet.
@Suppress("FunctionNaming")
internal class LoginProviderConfigTest {

    @Test
    fun `GIVEN Android WHEN reading the default configuration THEN Google and T-ID are shown and Apple is hidden`() {
        val config = defaultLoginProviderConfig()

        assertEquals(
            expected = LoginProviderConfig(providers = StubLoginProvider.stubAndroidProviders()),
            actual = config,
        )
    }
}
