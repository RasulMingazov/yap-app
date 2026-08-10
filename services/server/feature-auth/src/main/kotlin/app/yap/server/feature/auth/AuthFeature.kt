package app.yap.server.feature.auth

import app.yap.server.core.security.TokenService
import app.yap.server.feature.auth.api.authRoutes
import app.yap.server.feature.auth.identity.GoogleIdentityVerifier
import app.yap.server.feature.auth.identity.GoogleProviderConfig
import app.yap.server.feature.auth.identity.HttpGoogleTokenExchange
import app.yap.server.feature.auth.identity.IdentityVerifiers
import app.yap.server.feature.auth.identity.JwksSigningKeyProvider
import app.yap.server.feature.auth.identity.NonceHasher
import app.yap.server.feature.auth.persistence.ExposedAuthRepository
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.routing.Route
import java.net.URI
import java.time.Clock
import java.time.Duration
import java.util.concurrent.TimeUnit
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager

/**
 * The authentication feature as the application sees it: routes to install and one scheduled
 * scenario to run. The service, the provider verifiers, and the persistence adapter stay internal,
 * so the application composes the feature without depending on its models.
 *
 * The application decides [refreshTokenTtl] and owns the [tokenService], because both are shared
 * infrastructure; the provider configuration is read here, because it belongs to this feature.
 */
class AuthFeature(
    refreshTokenTtl: Duration,
    tokenService: TokenService,
    clock: Clock = Clock.systemUTC(),
    readVariable: (String) -> String? = System::getenv,
) {

    private val service = AuthService(
        clock = clock,
        identityVerifiers = identityVerifiers(
            clock = clock,
            nonceHasher = tokenService::hash,
            readVariable = readVariable,
        ),
        refreshTokenTtl = refreshTokenTtl,
        repository = ExposedAuthRepository(clock = clock, database = connectedDatabase()),
        tokenService = tokenService,
    )

    /**
     * Installs `POST /auth/challenge`, `POST /auth/login`, and `POST /auth/refresh`. There is no
     * log-out route.
     */
    fun registerRoutes(route: Route) {
        route.authRoutes(service)
    }

    /**
     * Removes expired challenges in its own committed transaction and returns how many were
     * removed. The application schedules it; no login attempt ever performs this cleanup.
     */
    suspend fun cleanupExpiredChallenges(): Int = service.cleanupExpiredChallenges()
}

/**
 * The providers configured for this deployment. Apple and T-ID have no configuration in this
 * iteration, so they resolve to no verifier and a request naming them fails as unavailable.
 */
private fun identityVerifiers(
    clock: Clock,
    nonceHasher: NonceHasher,
    readVariable: (String) -> String?,
): IdentityVerifiers = IdentityVerifiers(
    verifiers = listOfNotNull(
        GoogleProviderConfig.fromEnvironment(readVariable)?.let { config ->
            googleIdentityVerifier(clock = clock, config = config, nonceHasher = nonceHasher)
        },
    ),
)

private fun googleIdentityVerifier(
    clock: Clock,
    config: GoogleProviderConfig,
    nonceHasher: NonceHasher,
): GoogleIdentityVerifier = GoogleIdentityVerifier(
    clock = clock,
    config = config,
    nonceHasher = nonceHasher,
    signingKeys = JwksSigningKeyProvider(
        JwkProviderBuilder(URI.create(GOOGLE_JWKS_URI).toURL())
            .cached(JWKS_CACHE_SIZE, JWKS_CACHE_HOURS, TimeUnit.HOURS)
            .rateLimited(JWKS_REQUESTS_PER_MINUTE, 1, TimeUnit.MINUTES)
            .build(),
    ),
    // The browser fallback exchanges its code as the Android public client bound by PKCE.
    tokenExchange = HttpGoogleTokenExchange(clientId = config.androidClientId ?: config.serverClientId),
)

/**
 * The connection `core-database` bootstrapped for this process. The feature never opens its own
 * connection: it adapts the one the application already migrated and connected.
 */
private fun connectedDatabase(): Database = checkNotNull(TransactionManager.defaultDatabase) {
    "The database must be connected before the authentication feature is built"
}

private const val GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs"
private const val JWKS_CACHE_HOURS = 12L
private const val JWKS_CACHE_SIZE = 10L
private const val JWKS_REQUESTS_PER_MINUTE = 10L
