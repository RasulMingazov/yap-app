package app.yap.feature.auth.data.repository

import app.yap.contract.auth.LoginChallengeRequestDto
import app.yap.contract.auth.LoginRequestDto
import app.yap.feature.auth.data.identity.PreparedAttempt
import app.yap.feature.auth.data.identity.ProviderAuthResult
import app.yap.feature.auth.data.identity.ProviderFailureKind
import app.yap.feature.auth.data.identity.StubLoginProviderAdapter
import app.yap.feature.auth.data.identity.StubPreparedAttempt
import app.yap.feature.auth.data.local.StubSessionStorage
import app.yap.feature.auth.data.remote.StubAuthApi
import app.yap.feature.auth.data.remote.StubAuthDto
import app.yap.feature.auth.domain.entity.AccountId
import app.yap.feature.auth.domain.entity.LoginFailure
import app.yap.feature.auth.domain.entity.LoginOutcome
import app.yap.feature.auth.domain.entity.LoginProviderId
import app.yap.feature.auth.domain.entity.Session
import app.yap.feature.auth.domain.repository.SessionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DefaultSessionRepositoryLoginTest {

    @Test
    fun `GIVEN an enabled provider WHEN logging in THEN the attempt is prepared first and discarded last`() = runTest {
        val env = Environment()

        env.repository.logIn(LoginProviderId.Google)

        assertEquals(
            expected = listOf("prepareAttempt", "challenge", "authenticate", "login", "discard"),
            actual = env.journal,
        )
    }

    @Test
    fun `GIVEN a PKCE attempt WHEN requesting the challenge THEN only the code challenge leaves the device`() = runTest {
        val env = Environment()

        env.repository.logIn(LoginProviderId.Google)

        env.authApi.challengeCall.calledWith(
            LoginChallengeRequestDto(
                provider = "google",
                codeChallenge = StubPreparedAttempt.CODE_CHALLENGE,
                codeChallengeMethod = "S256",
            ),
        )
    }

    @Test
    fun `GIVEN an authorization code result WHEN logging in THEN the verifier reaches only the login request`() = runTest {
        val env = Environment(
            providerResult = ProviderAuthResult.Success(
                credential = StubPreparedAttempt.stubAuthorizationCodeCredential(),
            ),
        )

        env.repository.logIn(LoginProviderId.Google)

        env.authApi.loginCall.calledWith(
            LoginRequestDto(
                challengeId = StubAuthDto.CHALLENGE_ID,
                provider = "google",
                credentialType = "authorization_code",
                authorizationCode = StubPreparedAttempt.AUTHORIZATION_CODE,
                codeVerifier = StubPreparedAttempt.CODE_VERIFIER,
                redirectUri = StubPreparedAttempt.REDIRECT_URI,
            ),
        )
    }

    @Test
    fun `GIVEN a completed provider flow WHEN logging in THEN the new session is returned`() = runTest {
        val env = Environment()

        val outcome = env.repository.logIn(LoginProviderId.Google)

        assertEquals(
            expected = LoginOutcome.Success(Session(accountId = AccountId(StubAuthDto.ACCOUNT_ID))),
            actual = outcome,
        )
    }

    @Test
    fun `GIVEN a cancelled provider flow WHEN logging in THEN the attempt is discarded and cancellation is reported`() = runTest {
        val env = Environment(providerResult = ProviderAuthResult.Cancelled)

        val outcome = env.repository.logIn(LoginProviderId.Google)

        assertEquals(expected = LoginOutcome.Cancelled, actual = outcome)
        env.adapter.discardCall.called(1)
    }

    @Test
    fun `GIVEN a failed provider flow WHEN logging in THEN the attempt is discarded and the failure is reported`() = runTest {
        val env = Environment(
            providerResult = ProviderAuthResult.Failure(kind = ProviderFailureKind.Connectivity),
        )

        val outcome = env.repository.logIn(LoginProviderId.Google)

        assertEquals(
            expected = LoginOutcome.Failure(reason = LoginFailure.Connectivity),
            actual = outcome,
        )
        env.adapter.discardCall.called(1)
    }

    @Test
    fun `GIVEN a discarded attempt WHEN it is offered again THEN the login fails without a new challenge`() = runTest {
        val env = Environment()
        env.repository.logIn(LoginProviderId.Google)

        val outcome = env.repository.logIn(LoginProviderId.Google)

        assertEquals(
            expected = LoginOutcome.Failure(reason = LoginFailure.Provider),
            actual = outcome,
        )
        env.authApi.challengeCall.called(1)
    }

    @Test
    fun `GIVEN a provider without an adapter WHEN logging in THEN the attempt fails as a configuration problem`() = runTest {
        val env = Environment()

        val outcome = env.repository.logIn(LoginProviderId.Tid)

        assertEquals(
            expected = LoginOutcome.Failure(reason = LoginFailure.Configuration),
            actual = outcome,
        )
    }

    private class Environment(
        attempt: PreparedAttempt = StubPreparedAttempt.stubPreparedAttempt(),
        providerResult: ProviderAuthResult = ProviderAuthResult.Success(
            credential = StubPreparedAttempt.stubIdentityTokenCredential(),
        ),
    ) {

        val journal: MutableList<String> = mutableListOf()
        val adapter = StubLoginProviderAdapter(
            attempt = attempt,
            result = providerResult,
            journal = journal,
        )
        val authApi = StubAuthApi(journal = journal)
        val storage = StubSessionStorage()
        val repository: SessionRepository = DefaultSessionRepository(
            adapters = mapOf(LoginProviderId.Google to adapter),
            authApi = authApi,
            currentTime = { 0L },
            sessionStorage = storage,
        )
    }
}
