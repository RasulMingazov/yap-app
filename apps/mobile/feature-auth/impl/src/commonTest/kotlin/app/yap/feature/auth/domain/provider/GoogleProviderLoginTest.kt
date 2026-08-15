package app.yap.feature.auth.domain.provider

import app.yap.feature.auth.api.entity.AuthProvider
import app.yap.feature.auth.api.entity.AuthProviderType
import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.domain.repository.StubGoogleAuthRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

internal class GoogleProviderLoginTest {

    @Test
    fun `GIVEN the Google login path WHEN it is registered THEN it serves the Google provider`() {
        val providerLogin = GoogleProviderLogin(googleAuthRepository = StubGoogleAuthRepository())

        assertEquals(expected = AuthProviderType.GOOGLE, actual = providerLogin.type)
    }

    @Test
    fun `GIVEN a provider call that succeeds WHEN it concludes THEN its own outcome stands`() = runTest {
        val providerLogin = GoogleProviderLogin(googleAuthRepository = StubGoogleAuthRepository(outcome = LoginOutcome.Success))

        assertEquals(expected = LoginOutcome.Success, actual = providerLogin.login())
    }

    @Test
    fun `GIVEN a provider call that fails WHEN it concludes THEN its own outcome stands`() = runTest {
        val providerLogin = GoogleProviderLogin(googleAuthRepository = StubGoogleAuthRepository(outcome = LoginOutcome.Failed))

        assertEquals(expected = LoginOutcome.Failed, actual = providerLogin.login())
    }
}
