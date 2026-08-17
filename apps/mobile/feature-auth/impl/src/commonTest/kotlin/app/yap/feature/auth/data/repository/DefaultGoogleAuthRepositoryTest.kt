package app.yap.feature.auth.data.repository

import app.yap.contract.auth.GoogleAuthorizationCodeDto
import app.yap.contract.auth.GoogleCredentialsDto
import app.yap.core.network.ApiError
import app.yap.core.network.ApiResult
import app.yap.feature.auth.data.identity.GoogleCredential
import app.yap.feature.auth.data.identity.LoginCancelledException
import app.yap.feature.auth.api.entity.AuthSessionState
import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.api.entity.UserId
import app.yap.feature.auth.data.CurrentTime
import app.yap.feature.auth.data.SessionStore
import app.yap.feature.auth.data.identity.StubGoogleCredentialProvider
import app.yap.feature.auth.data.local.StubSession
import app.yap.feature.auth.data.local.StubSessionStorage
import app.yap.feature.auth.data.remote.StubAuthRemoteDataSource
import app.yap.feature.auth.domain.repository.GoogleAuthRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

internal class DefaultGoogleAuthRepositoryTest {

    @Test
    fun `GIVEN the device returns an id token WHEN logging in THEN the native Google door is called`() = runTest {
        val env = Environment()

        env.repository.login()

        env.remoteDataSource.loginWithGoogleIdTokenCall.called(times = 1)
        env.remoteDataSource.loginWithGoogleAuthorizationCodeCall.notCalled()
    }

    @Test
    fun `GIVEN the device returns an authorization code WHEN logging in THEN the fallback door is called`() = runTest {
        val env = Environment(
            credential = GoogleCredential.AuthorizationCode(
                code = CODE,
                codeVerifier = CODE_VERIFIER,
                redirectUri = REDIRECT_URI,
            ),
        )

        env.repository.login()

        env.remoteDataSource.loginWithGoogleAuthorizationCodeCall.calledWith(
            GoogleAuthorizationCodeDto(code = CODE, codeVerifier = CODE_VERIFIER, redirectUri = REDIRECT_URI),
        )
        env.remoteDataSource.loginWithGoogleIdTokenCall.notCalled()
    }

    @Test
    fun `GIVEN a login attempt WHEN it runs THEN the nonce handed to the device is the one submitted`() = runTest {
        val env = Environment()

        env.repository.login()

        env.credentialProvider.requestCredentialCall.calledWith(NonceGenerator.FIRST_NONCE)
        env.remoteDataSource.loginWithGoogleIdTokenCall.calledWith(
            GoogleCredentialsDto(idToken = ID_TOKEN, nonce = NonceGenerator.FIRST_NONCE),
        )
    }

    @Test
    fun `GIVEN one attempt has run WHEN a second starts THEN a fresh nonce is generated`() = runTest {
        val env = Environment()

        env.repository.login()
        env.repository.login()

        env.credentialProvider.requestCredentialCall.calledWith(NonceGenerator.SECOND_NONCE)
    }

    @Test
    fun `GIVEN the server issues a session WHEN logging in THEN it is stored and logged in is published`() = runTest {
        val env = Environment()

        val outcome = env.repository.login()

        assertEquals(expected = LoginOutcome.Success, actual = outcome)
        env.sessionStorage.writeCall.calledWith(StubSession.stubSessionLocal())
        assertEquals(
            expected = AuthSessionState.LoggedIn(UserId(StubSession.USER_ID)),
            actual = env.sessionStore.sessionState.value,
        )
    }

    @Test
    fun `GIVEN the user dismisses the provider WHEN logging in THEN the attempt is cancelled silently`() = runTest {
        val env = Environment()
        env.credentialProvider.requestCredentialCall.throws(LoginCancelledException())

        val outcome = env.repository.login()

        assertEquals(expected = LoginOutcome.Cancelled, actual = outcome)
        env.sessionStorage.writeCall.notCalled()
    }

    @Test
    fun `GIVEN the provider fails for any other reason WHEN logging in THEN the attempt reports a failure`() =
        runTest {
            val env = Environment()
            env.credentialProvider.requestCredentialCall.throws(IllegalStateException("provider misconfigured"))

            val outcome = env.repository.login()

            assertEquals(expected = LoginOutcome.Failed, actual = outcome)
            env.sessionStorage.writeCall.notCalled()
        }

    @Test
    fun `GIVEN the server refuses the confirmation WHEN logging in THEN the attempt reports a failure`() = runTest {
        val env = Environment()
        env.remoteDataSource.loginWithGoogleIdTokenCall.returns(ApiResult.Failure(ApiError.Rejected(code = null)))

        val outcome = env.repository.login()

        assertEquals(expected = LoginOutcome.Failed, actual = outcome)
        env.sessionStorage.writeCall.notCalled()
    }

    @Test
    fun `GIVEN the server cannot be reached WHEN logging in THEN the attempt fails`() = runTest {
        val env = Environment()
        env.remoteDataSource.loginWithGoogleIdTokenCall.returns(ApiResult.Failure(ApiError.Unavailable))

        val outcome = env.repository.login()

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
        credential: GoogleCredential = GoogleCredential.IdToken(value = ID_TOKEN),
    ) {

        val credentialProvider = StubGoogleCredentialProvider(credential = credential)
        val remoteDataSource = StubAuthRemoteDataSource()
        val sessionStorage = StubSessionStorage(session = null)
        val sessionStore = SessionStore(
            currentTime = CurrentTime { StubSession.NOW_EPOCH_SECONDS },
            sessionStorage = sessionStorage,
        )
        val repository: GoogleAuthRepository = DefaultGoogleAuthRepository(
            authRemoteDataSource = remoteDataSource,
            googleCredentialProvider = credentialProvider,
            nonceGenerator = NonceGenerator(),
            sessionStore = sessionStore,
        )
    }

    private companion object {
        const val ID_TOKEN = "google-id-token"
        const val CODE = "auth-code"
        const val CODE_VERIFIER = "verifier"
        const val REDIRECT_URI = "com.googleusercontent.apps.android:/oauth2redirect"
    }
}
