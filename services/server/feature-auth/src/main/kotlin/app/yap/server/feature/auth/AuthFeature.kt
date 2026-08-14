package app.yap.server.feature.auth

import app.yap.server.core.security.TokenService
import app.yap.server.feature.auth.api.authRoutes
import app.yap.server.feature.auth.identity.GoogleAuthConfig
import app.yap.server.feature.auth.identity.GoogleCodeExchanger
import app.yap.server.feature.auth.identity.GoogleIdentityVerifier
import app.yap.server.feature.auth.identity.googleJwkProvider
import app.yap.server.feature.auth.persistence.AuthPersistenceRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.routing.Route
import java.time.Clock
import kotlinx.serialization.json.Json

class AuthFeature(
    googleAuthConfig: GoogleAuthConfig,
    refreshTokenTtlSeconds: Long,
    tokenService: TokenService,
    private val httpClient: HttpClient = defaultHttpClient(),
) : AutoCloseable {

    private val googleIdentityVerifier = GoogleIdentityVerifier(
        googleAuthConfig = googleAuthConfig,
        jwkProvider = googleJwkProvider(),
    )

    private val authService = AuthService(
        authPersistence = AuthPersistenceRepository(),
        clock = Clock.systemUTC(),
        googleCodeExchanger = GoogleCodeExchanger(
            googleAuthConfig = googleAuthConfig,
            googleIdentityVerifier = googleIdentityVerifier,
            httpClient = httpClient,
        ),
        googleIdentityVerifier = googleIdentityVerifier,
        refreshTokenTtlSeconds = refreshTokenTtlSeconds,
        tokenService = tokenService,
    )

    fun install(route: Route) {
        route.authRoutes(authService)
    }

    override fun close() {
        httpClient.close()
    }

    private companion object {

        fun defaultHttpClient(): HttpClient = HttpClient(CIO) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}
