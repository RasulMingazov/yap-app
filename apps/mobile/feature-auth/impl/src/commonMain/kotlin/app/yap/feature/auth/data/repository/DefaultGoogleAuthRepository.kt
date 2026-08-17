package app.yap.feature.auth.data.repository

import app.yap.contract.auth.GoogleAuthorizationCodeDto
import app.yap.contract.auth.GoogleCredentialsDto
import app.yap.contract.auth.SessionDto
import app.yap.core.common.coroutines.runSuspendCatching
import app.yap.core.network.ApiResult
import app.yap.feature.auth.data.identity.GoogleCredential
import app.yap.feature.auth.data.identity.GoogleCredentialProvider
import app.yap.feature.auth.data.identity.LoginCancelledException
import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.data.SessionStore
import app.yap.feature.auth.data.identity.NonceGenerator
import app.yap.feature.auth.data.remote.AuthRemoteDataSource
import app.yap.feature.auth.domain.repository.GoogleAuthRepository

internal class DefaultGoogleAuthRepository(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val googleCredentialProvider: GoogleCredentialProvider,
    private val nonceGenerator: NonceGenerator,
    private val sessionStore: SessionStore,
) : GoogleAuthRepository {

    override suspend fun login(): LoginOutcome {
        val nonce = nonceGenerator.generate()

        return runSuspendCatching { requestSession(nonce) }.fold(
            onSuccess = { result -> outcomeOf(result) },
            onFailure = { error ->
                if (error is LoginCancelledException) LoginOutcome.Cancelled else LoginOutcome.Failed
            },
        )
    }

    private suspend fun requestSession(nonce: String): ApiResult<SessionDto> =
        when (val credential = googleCredentialProvider.requestCredential(nonce)) {
            is GoogleCredential.IdToken -> authRemoteDataSource.loginWithGoogleIdToken(
                GoogleCredentialsDto(idToken = credential.value, nonce = nonce),
            )

            is GoogleCredential.AuthorizationCode ->
                authRemoteDataSource.loginWithGoogleAuthorizationCode(
                    GoogleAuthorizationCodeDto(
                        code = credential.code,
                        codeVerifier = credential.codeVerifier,
                        redirectUri = credential.redirectUri,
                    ),
                )
        }

    private suspend fun outcomeOf(result: ApiResult<SessionDto>): LoginOutcome = when (result) {
        is ApiResult.Success -> {
            sessionStore.write(result.value)
            LoginOutcome.Success
        }

        is ApiResult.Failure -> LoginOutcome.Failed
    }
}
