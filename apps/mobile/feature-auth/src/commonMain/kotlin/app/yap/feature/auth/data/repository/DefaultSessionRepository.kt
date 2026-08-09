package app.yap.feature.auth.data.repository

import app.yap.contract.auth.LoginChallengeDto
import app.yap.contract.auth.RefreshRequestDto
import app.yap.feature.auth.data.identity.LoginProviderAdapter
import app.yap.feature.auth.data.identity.PreparedAttempt
import app.yap.feature.auth.data.identity.ProviderAuthResult
import app.yap.feature.auth.data.identity.ProviderCredential
import app.yap.feature.auth.data.local.SessionDb
import app.yap.feature.auth.data.local.SessionStorage
import app.yap.feature.auth.data.mapper.toChallenge
import app.yap.feature.auth.data.mapper.toChallengeRequest
import app.yap.feature.auth.data.mapper.toDb
import app.yap.feature.auth.data.mapper.toDomain
import app.yap.feature.auth.data.mapper.toLoginRequest
import app.yap.feature.auth.data.remote.AuthApi
import app.yap.feature.auth.data.remote.AuthApiFailureKind
import app.yap.feature.auth.data.remote.AuthApiResult
import app.yap.feature.auth.data.time.CurrentTime
import app.yap.feature.auth.domain.entity.LoginFailure
import app.yap.feature.auth.domain.entity.LoginOutcome
import app.yap.feature.auth.domain.entity.LoginProviderId
import app.yap.feature.auth.domain.entity.Session
import app.yap.feature.auth.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns the whole session lifecycle: the prepared-attempt sequence of a login, secure persistence,
 * and single-flight refresh. Rotated credentials are stored before they become observable (R-058).
 */
internal class DefaultSessionRepository(
    private val adapters: Map<LoginProviderId, LoginProviderAdapter>,
    private val authApi: AuthApi,
    private val currentTime: CurrentTime,
    private val sessionStorage: SessionStorage,
) : SessionRepository, SessionCredentials {

    private val loadMutex = Mutex()
    private val refreshMutex = Mutex()
    private val sessionState = MutableStateFlow<SessionDb?>(null)
    private val usedAttemptIds = mutableSetOf<String>()
    private var isLoaded = false

    override fun observe(): Flow<Session?> = sessionState.map { session -> session?.toDomain() }

    override suspend fun get(forceUpdate: Boolean): Session? {
        val stored = loadedSession() ?: return null
        val hasExpiredAccess = currentTime.epochSeconds() >= stored.accessTokenExpiresAtEpochSeconds

        return when {
            !forceUpdate || !hasExpiredAccess -> stored.toDomain()
            else -> when (val outcome = refresh(rejectedAccessToken = stored.accessToken)) {
                is RefreshOutcome.Cleared -> null
                is RefreshOutcome.Preserved -> stored.toDomain()
                is RefreshOutcome.Rotated -> outcome.session.toDomain()
            }
        }
    }

    override suspend fun logIn(providerId: LoginProviderId): LoginOutcome {
        val adapter = adapters[providerId]
            ?: return LoginOutcome.Failure(reason = LoginFailure.Configuration)

        val attempt = adapter.prepareAttempt()
        return try {
            runAttempt(adapter = adapter, attempt = attempt, providerId = providerId)
        } finally {
            adapter.discard(attempt)
        }
    }

    override suspend fun accessToken(): String? = loadedSession()?.accessToken

    override suspend fun refreshedAccessToken(rejectedAccessToken: String): String? =
        when (val outcome = refresh(rejectedAccessToken = rejectedAccessToken)) {
            is RefreshOutcome.Cleared -> null
            is RefreshOutcome.Preserved -> null
            is RefreshOutcome.Rotated -> outcome.session.accessToken
        }

    private suspend fun runAttempt(
        adapter: LoginProviderAdapter,
        attempt: PreparedAttempt,
        providerId: LoginProviderId,
    ): LoginOutcome {
        if (!usedAttemptIds.add(attempt.attemptId)) {
            return LoginOutcome.Failure(reason = LoginFailure.Provider)
        }

        val request = attempt.toChallengeRequest(providerId = providerId)
        return when (val challengeResult = authApi.challenge(request)) {
            is AuthApiResult.Failure -> LoginOutcome.Failure(reason = challengeResult.kind.toDomain())
            is AuthApiResult.Success -> authenticate(
                adapter = adapter,
                attempt = attempt,
                challenge = challengeResult.value,
                providerId = providerId,
            )
        }
    }

    private suspend fun authenticate(
        adapter: LoginProviderAdapter,
        attempt: PreparedAttempt,
        challenge: LoginChallengeDto,
        providerId: LoginProviderId,
    ): LoginOutcome = when (
        val authResult = adapter.authenticate(attempt, challenge.toChallenge())
    ) {
        is ProviderAuthResult.Cancelled -> LoginOutcome.Cancelled
        is ProviderAuthResult.Failure -> LoginOutcome.Failure(reason = authResult.kind.toDomain())
        is ProviderAuthResult.Success -> submitLogin(
            challengeId = challenge.challengeId,
            credential = authResult.credential,
            providerId = providerId,
        )
    }

    private suspend fun submitLogin(
        challengeId: String,
        credential: ProviderCredential,
        providerId: LoginProviderId,
    ): LoginOutcome {
        val request = credential.toLoginRequest(challengeId = challengeId, providerId = providerId)
        return when (val loginResult = authApi.login(request)) {
            is AuthApiResult.Failure -> LoginOutcome.Failure(reason = loginResult.kind.toDomain())
            is AuthApiResult.Success -> {
                val session = loginResult.value.toDb()
                persist(session)
                LoginOutcome.Success(session = session.toDomain())
            }
        }
    }

    private suspend fun refresh(rejectedAccessToken: String): RefreshOutcome = refreshMutex.withLock {
        val current = loadedSession() ?: return@withLock RefreshOutcome.Cleared
        if (current.accessToken != rejectedAccessToken) return@withLock RefreshOutcome.Rotated(current)

        val result = authApi.refresh(RefreshRequestDto(refreshToken = current.refreshToken))
        when (result) {
            is AuthApiResult.Failure -> when (result.kind) {
                AuthApiFailureKind.Rejected -> {
                    sessionStorage.clear()
                    sessionState.value = null
                    RefreshOutcome.Cleared
                }

                AuthApiFailureKind.Unavailable -> RefreshOutcome.Preserved
            }

            is AuthApiResult.Success -> {
                val rotated = result.value.toDb()
                persist(rotated)
                RefreshOutcome.Rotated(rotated)
            }
        }
    }

    private suspend fun loadedSession(): SessionDb? = loadMutex.withLock {
        if (!isLoaded) {
            sessionState.value = sessionStorage.read()
            isLoaded = true
        }
        sessionState.value
    }

    private suspend fun persist(session: SessionDb) {
        sessionStorage.write(session)
        isLoaded = true
        sessionState.value = session
    }

    private sealed interface RefreshOutcome {

        data object Cleared : RefreshOutcome

        data object Preserved : RefreshOutcome

        data class Rotated(val session: SessionDb) : RefreshOutcome
    }
}
