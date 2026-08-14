package app.yap.feature.auth.data.repository

import app.yap.contract.auth.GoogleAuthorizationCodeDto
import app.yap.contract.auth.GoogleCredentialsDto
import app.yap.feature.auth.api.GoogleCredential
import app.yap.feature.auth.api.LoginCancelledException
import app.yap.feature.auth.api.entity.AuthState
import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.api.entity.UserId
import app.yap.feature.auth.data.AuthStateSource
import app.yap.feature.auth.data.CurrentTime
import app.yap.feature.auth.data.StubAccessTokenProvider
import app.yap.feature.auth.data.identity.StubGoogleCredentialProvider
import app.yap.feature.auth.data.local.SessionLocal
import app.yap.feature.auth.data.local.StubSession
import app.yap.feature.auth.data.local.StubSessionStorage
import app.yap.feature.auth.data.remote.AuthRemoteFailure
import app.yap.feature.auth.data.remote.StubAuthRemoteDataSource
import app.yap.feature.auth.domain.repository.AuthRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

internal class DefaultAuthRepositoryTest {

    @Test
    fun `GIVEN a stored session WHEN auth state is observed THEN it starts unknown and then logged in`() = runTest {
        val env = Environment(storedSession = StubSession.stubSessionLocal())

        val states = env.repository.observe().take(2).toList()

        assertEquals(
            expected = listOf(AuthState.Unknown, AuthState.LoggedIn(UserId(StubSession.USER_ID))),
            actual = states,
        )
    }

    @Test
    fun `GIVEN no stored session WHEN auth state is observed THEN it starts unknown and then logged out`() = runTest {
        val env = Environment(storedSession = null)

        val states = env.repository.observe().take(2).toList()

        assertEquals(
            expected = listOf(AuthState.Unknown, AuthState.LoggedOut),
            actual = states,
        )
    }

    @Test
    fun `GIVEN a stored session WHEN auth state is observed twice THEN storage is read once`() = runTest {
        val env = Environment(storedSession = StubSession.stubSessionLocal())

        env.repository.observe().take(2).toList()
        env.repository.observe().take(1).toList()

        env.sessionStorage.readCall.called(times = 1)
    }

    @Test
    fun `GIVEN a stored session inside its window WHEN the launch decision is made THEN no request is made`() =
        runTest {
            val env = Environment(storedSession = StubSession.stubSessionLocal())

            val state = env.repository.observe().first { authState -> authState !is AuthState.Unknown }

            assertEquals(expected = AuthState.LoggedIn(UserId(StubSession.USER_ID)), actual = state)
            env.remoteDataSource.refreshCall.notCalled()
        }

    @Test
    fun `GIVEN a stored session past its own expiry WHEN the launch decision is made THEN no request is made`() =
        runTest {
            val env = Environment(
                storedSession = StubSession.stubSessionLocal(),
                nowEpochSeconds = StubSession.REFRESH_TOKEN_EXPIRES_AT_EPOCH_SECONDS + 1,
            )

            val state = env.repository.observe().first { authState -> authState !is AuthState.Unknown }

            assertEquals(expected = AuthState.LoggedOut, actual = state)
            env.remoteDataSource.refreshCall.notCalled()
            env.sessionStorage.clearCall.called(times = 1)
        }

    @Test
    fun `GIVEN a device clock moved backwards WHEN the launch decision is made THEN it is still made locally`() =
        runTest {
            val env = Environment(
                storedSession = StubSession.stubSessionLocal(),
                nowEpochSeconds = 0L,
            )

            val state = env.repository.observe().first { authState -> authState !is AuthState.Unknown }

            assertEquals(expected = AuthState.LoggedIn(UserId(StubSession.USER_ID)), actual = state)
            env.remoteDataSource.refreshCall.notCalled()
        }

    @Test
    fun `GIVEN a device clock moved forwards WHEN the launch decision is made THEN the session reads as expired`() =
        runTest {
            val env = Environment(
                storedSession = StubSession.stubSessionLocal(),
                nowEpochSeconds = StubSession.REFRESH_TOKEN_EXPIRES_AT_EPOCH_SECONDS * 2,
            )

            val state = env.repository.observe().first { authState -> authState !is AuthState.Unknown }

            assertEquals(expected = AuthState.LoggedOut, actual = state)
            env.remoteDataSource.refreshCall.notCalled()
        }

    @Test
    fun `GIVEN a stored session WHEN its access token lifetime is read THEN it is measured against now`() = runTest {
        val env = Environment(storedSession = StubSession.stubSessionLocal())

        val lifetimeSeconds = env.repository.accessTokenLifetimeSeconds()

        assertEquals(
            expected = StubSession.ACCESS_TOKEN_EXPIRES_AT_EPOCH_SECONDS - StubSession.NOW_EPOCH_SECONDS,
            actual = lifetimeSeconds,
        )
    }

    @Test
    fun `GIVEN no stored session WHEN its access token lifetime is read THEN there is none to report`() = runTest {
        val env = Environment(storedSession = null)

        assertNull(actual = env.repository.accessTokenLifetimeSeconds())
    }

    @Test
    fun `GIVEN a stored session WHEN the session is renewed THEN the stored token is presented as rejected`() =
        runTest {
            val env = Environment(storedSession = StubSession.stubSessionLocal())

            env.repository.renewSession()

            env.accessTokenProvider.getAccessTokenCall.calledWith(StubSession.ACCESS_TOKEN)
        }

    @Test
    fun `GIVEN no stored session WHEN the session is renewed THEN nothing is asked of the server`() = runTest {
        val env = Environment(storedSession = null)

        env.repository.renewSession()

        env.accessTokenProvider.getAccessTokenCall.notCalled()
    }

    @Test
    fun `GIVEN the device returns an id token WHEN logging in THEN the native Google door is called`() = runTest {
        val env = Environment(storedSession = null)

        env.repository.loginWithGoogle()

        env.remoteDataSource.loginWithGoogleIdTokenCall.called(times = 1)
        env.remoteDataSource.loginWithGoogleAuthorizationCodeCall.notCalled()
    }

    @Test
    fun `GIVEN the device returns an authorization code WHEN logging in THEN the fallback door is called`() = runTest {
        val env = Environment(
            storedSession = null,
            credential = GoogleCredential.AuthorizationCode(
                code = CODE,
                codeVerifier = CODE_VERIFIER,
                redirectUri = REDIRECT_URI,
            ),
        )

        env.repository.loginWithGoogle()

        env.remoteDataSource.loginWithGoogleAuthorizationCodeCall.calledWith(
            GoogleAuthorizationCodeDto(code = CODE, codeVerifier = CODE_VERIFIER, redirectUri = REDIRECT_URI),
        )
        env.remoteDataSource.loginWithGoogleIdTokenCall.notCalled()
    }

    @Test
    fun `GIVEN a login attempt WHEN it runs THEN the nonce handed to the device is the one submitted`() = runTest {
        val env = Environment(storedSession = null)

        env.repository.loginWithGoogle()

        env.credentialProvider.requestCredentialCall.calledWith(NonceGenerator.FIRST_NONCE)
        env.remoteDataSource.loginWithGoogleIdTokenCall.calledWith(
            GoogleCredentialsDto(idToken = ID_TOKEN, nonce = NonceGenerator.FIRST_NONCE),
        )
    }

    @Test
    fun `GIVEN one attempt has run WHEN a second starts THEN a fresh nonce is generated`() = runTest {
        val env = Environment(storedSession = null)

        env.repository.loginWithGoogle()
        env.repository.loginWithGoogle()

        env.credentialProvider.requestCredentialCall.calledWith(NonceGenerator.SECOND_NONCE)
    }

    @Test
    fun `GIVEN the server issues a session WHEN logging in THEN it is stored and logged in is published`() = runTest {
        val env = Environment(storedSession = null)

        val outcome = env.repository.loginWithGoogle()

        assertEquals(expected = LoginOutcome.Success, actual = outcome)
        env.sessionStorage.writeCall.calledWith(StubSession.stubSessionLocal())
        assertEquals(
            expected = AuthState.LoggedIn(UserId(StubSession.USER_ID)),
            actual = env.repository.observe().first { state -> state !is AuthState.Unknown },
        )
    }

    @Test
    fun `GIVEN the user dismisses the provider WHEN logging in THEN the attempt is cancelled silently`() = runTest {
        val env = Environment(storedSession = null)
        env.credentialProvider.requestCredentialCall.throws(LoginCancelledException())

        val outcome = env.repository.loginWithGoogle()

        assertEquals(expected = LoginOutcome.Cancelled, actual = outcome)
        env.sessionStorage.writeCall.notCalled()
    }

    @Test
    fun `GIVEN the provider fails for any other reason WHEN logging in THEN the attempt reports a failure`() =
        runTest {
            val env = Environment(storedSession = null)
            env.credentialProvider.requestCredentialCall.throws(IllegalStateException("provider misconfigured"))

            val outcome = env.repository.loginWithGoogle()

            assertEquals(expected = LoginOutcome.Failed, actual = outcome)
            env.sessionStorage.writeCall.notCalled()
        }

    @Test
    fun `GIVEN the server refuses the confirmation WHEN logging in THEN the attempt reports a failure`() = runTest {
        val env = Environment(storedSession = null)
        env.remoteDataSource.loginWithGoogleIdTokenCall.throws(AuthRemoteFailure.Rejected())

        val outcome = env.repository.loginWithGoogle()

        assertEquals(expected = LoginOutcome.Failed, actual = outcome)
        env.sessionStorage.writeCall.notCalled()
    }

    @Test
    fun `GIVEN the server cannot be reached WHEN logging in THEN the attempt fails`() = runTest {
        val env = Environment(storedSession = null)
        env.remoteDataSource.loginWithGoogleIdTokenCall.throws(AuthRemoteFailure.Unavailable())

        val outcome = env.repository.loginWithGoogle()

        assertEquals(expected = LoginOutcome.Failed, actual = outcome)
        env.sessionStorage.writeCall.notCalled()
    }

    private class NonceGenerator : app.yap.feature.auth.data.identity.NonceGenerator {

        private var count = 0

        override fun generate(): String {
            count += 1
            return "nonce-$count"
        }

        companion object {
            const val FIRST_NONCE = "nonce-1"
            const val SECOND_NONCE = "nonce-2"
        }
    }

    private class Environment(
        storedSession: SessionLocal?,
        credential: GoogleCredential = GoogleCredential.IdToken(value = ID_TOKEN),
        nowEpochSeconds: Long = StubSession.NOW_EPOCH_SECONDS,
    ) {

        val credentialProvider = StubGoogleCredentialProvider(credential = credential)
        val remoteDataSource = StubAuthRemoteDataSource()
        val sessionStorage = StubSessionStorage(session = storedSession)
        val accessTokenProvider = StubAccessTokenProvider()
        val authStateSource = AuthStateSource()
        val repository: AuthRepository = DefaultAuthRepository(
            accessTokenProvider = accessTokenProvider,
            authRemoteDataSource = remoteDataSource,
            authStateSource = authStateSource,
            currentTime = CurrentTime { nowEpochSeconds },
            googleCredentialProvider = credentialProvider,
            nonceGenerator = NonceGenerator(),
            sessionStorage = sessionStorage,
        )
    }

    private companion object {
        const val ID_TOKEN = "google-id-token"
        const val CODE = "auth-code"
        const val CODE_VERIFIER = "verifier"
        const val REDIRECT_URI = "com.googleusercontent.apps.android:/oauth2redirect"
    }
}
