package app.yap.feature.auth.di

import app.yap.feature.auth.domain.entity.StubLoginProvider
import kotlin.test.Test
import kotlin.test.assertEquals

internal class LoginProviderConfigTest {

    @Test
    fun `GIVEN iOS WHEN reading the default configuration THEN Google Apple and T-ID are all shown`() {
        val config = defaultLoginProviderConfig()

        assertEquals(
            expected = LoginProviderConfig(providers = StubLoginProvider.stubIosProviders()),
            actual = config,
        )
    }
}
