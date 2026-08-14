package app.yap.server.app

import app.yap.server.core.config.AppConfig
import app.yap.server.core.config.AppConfigLoader
import app.yap.server.core.database.DatabaseFactory
import app.yap.server.core.security.JwtTokenService
import app.yap.server.feature.auth.AuthFeature
import app.yap.server.feature.auth.identity.GoogleAuthConfig
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main() {
    val config = AppConfigLoader.load()
    DatabaseFactory.init(config.database)

    embeddedServer(Netty, port = config.port) { serverModule(config) }
        .start(wait = true)
}

internal fun Application.serverModule(
    config: AppConfig,
    authFeature: AuthFeature = authFeature(config),
) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; explicitNulls = false })
    }
    installErrorMapping()
    installAuthRateLimit(
        requestsPerMinute = config.authRateLimitRequestsPerMinute,
        trustProxyHeaders = config.trustProxyHeaders,
    )

    routing {
        get("/health") { call.respondText("ok") }

        rateLimit(AUTH_RATE_LIMIT_NAME) {
            authFeature.install(this)
        }
    }

    monitor.subscribe(ApplicationStopped) {
        authFeature.close()
        DatabaseFactory.close()
    }
}

internal fun authFeature(config: AppConfig): AuthFeature = AuthFeature(
    googleAuthConfig = GoogleAuthConfig(
        androidClientId = config.googleAndroidClientId,
        iosClientId = config.googleIosClientId,
        webClientId = config.googleWebClientId,
    ),
    refreshTokenTtlSeconds = config.auth.refreshTokenTtlSeconds,
    tokenService = JwtTokenService(
        jwtSecret = config.auth.jwtSecret,
        jwtIssuer = config.auth.jwtIssuer,
        jwtAudience = config.auth.jwtAudience,
        accessTokenTtlSeconds = config.auth.accessTokenTtlSeconds,
    ),
)
