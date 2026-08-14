package app.yap.feature.auth.data.repository

import app.yap.contract.auth.GoogleAuthorizationCodeDto
import app.yap.contract.auth.GoogleCredentialsDto
import app.yap.contract.auth.SessionDto
import app.yap.feature.auth.api.GoogleCredential
import app.yap.feature.auth.api.GoogleCredentialProvider
import app.yap.feature.auth.api.LoginCancelledException
import app.yap.feature.auth.api.entity.AuthState
import app.yap.feature.auth.api.entity.LoginOutcome
import app.yap.feature.auth.data.AuthStateSource
import app.yap.feature.auth.data.CurrentTime
import app.yap.feature.auth.data.identity.NonceGenerator
import app.yap.feature.auth.data.local.SessionLocal
import app.yap.feature.auth.data.local.SessionStorage
import app.yap.feature.auth.data.mapper.toDomain
import app.yap.feature.auth.data.mapper.toLocal
import app.yap.feature.auth.data.remote.AuthRemoteDataSource
import app.yap.core.common.coroutines.runSuspendCatching
import app.yap.core.common.network.AccessTokenProvider
import app.yap.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DefaultAuthRepository(
    private val accessTokenProvider: AccessTokenProvider,
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val authStateSource: AuthStateSource,
    private val currentTime: CurrentTime,
    private val googleCredentialProvider: GoogleCredentialProvider,
    private val nonceGenerator: NonceGenerator,
    private val sessionStorage: SessionStorage,
) : AuthRepository {

    private val readMutex = Mutex()

    private var hasReadStorage = false

    override fun observe(): Flow<AuthState> = flow {
        emit(authStateSource.authState.value)
        resolveFromStorage()
        emitAll(authStateSource.authState)
    }.distinctUntilChanged()

    override suspend fun loginWithGoogle(): LoginOutcome {
        val nonce = nonceGenerator.generate()

        return runSuspendCatching { requestSession(nonce) }.fold(
            onSuccess = { session ->
                store(session)
                LoginOutcome.Success
            },
            onFailure = { error ->
                if (error is LoginCancelledException) LoginOutcome.Cancelled else LoginOutcome.Failed
            },
        )
    }

    override suspend fun accessTokenLifetimeSeconds(): Long? {
        val stored = sessionStorage.read() ?: return null
        return stored.accessTokenExpiresAtEpochSeconds - currentTime.epochSeconds()
    }

    override suspend fun renewSession() {
        val stored = sessionStorage.read() ?: return
        accessTokenProvider.getAccessToken(rejectedAccessToken = stored.accessToken)
    }

    private suspend fun requestSession(nonce: String): SessionDto =
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

    private suspend fun store(session: SessionDto) {
        val stored = session.toLocal()
        sessionStorage.write(stored)
        hasReadStorage = true
        authStateSource.publish(stored.toDomain())
    }

    private suspend fun resolveFromStorage() {
        readMutex.withLock {
            if (hasReadStorage) return
            hasReadStorage = true
            authStateSource.publish(
                sessionStorage.read()?.takeUnlessExpired()?.toDomain() ?: AuthState.LoggedOut,
            )
        }
    }

    private suspend fun SessionLocal.takeUnlessExpired(): SessionLocal? {
        if (refreshTokenExpiresAtEpochSeconds > currentTime.epochSeconds()) return this

        sessionStorage.clear()
        return null
    }
}
