package app.yap.server

import app.yap.server.core.config.AppConfigLoader
import app.yap.server.core.database.DatabaseFactory
import app.yap.server.core.security.JwtTokenService
import app.yap.server.feature.auth.AuthFeature
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Builds the object graph by hand: configuration, the migrated database, the token service, and the
 * authentication feature that owns everything behind its own routes.
 */
fun main() {
    val config = AppConfigLoader.load()
    DatabaseFactory.init(config.database)
    val auth = AuthFeature(
        refreshTokenTtl = Duration.ofSeconds(config.auth.refreshTokenTtlSeconds),
        tokenService = JwtTokenService(
            jwtSecret = config.auth.jwtSecret,
            jwtIssuer = config.auth.jwtIssuer,
            jwtAudience = config.auth.jwtAudience,
            accessTokenTtlSeconds = config.auth.accessTokenTtlSeconds,
        ),
    )

    val server = embeddedServer(Netty, port = config.port) {
        serverModule(auth = auth, isDatabaseReady = DatabaseFactory::isReady)
    }
    server.addShutdownHook(DatabaseFactory::close)
    server.start(wait = true)
}

internal fun Application.serverModule(auth: AuthFeature, isDatabaseReady: suspend () -> Boolean) {
    installSharedPlugins()
    routing {
        healthRoute(isDatabaseReady)
        auth.registerRoutes(this)
    }
    scheduleChallengeCleanup(auth)
}

/**
 * The plugins every route shares. Feature failures are already translated by the routes that own
 * them, so this handler exists for the unexpected ones: it answers with a status and nothing else,
 * because an exception message may quote the request that carried a credential.
 */
internal fun Application.installSharedPlugins() {
    install(ContentNegotiation) { json() }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled failure while answering ${call.request.local.uri}", cause)
            call.respondText(text = "Internal server error", status = HttpStatusCode.InternalServerError)
        }
    }
}

/** Reports whether this instance can serve traffic, which it cannot without its database. */
internal fun Route.healthRoute(isDatabaseReady: suspend () -> Boolean) {
    get("/health") {
        if (!isDatabaseReady()) {
            call.respondText(text = "unavailable", status = HttpStatusCode.ServiceUnavailable)
            return@get
        }

        call.respondText(text = "ok")
    }
}

/**
 * Expired challenges are removed by this scheduled scenario alone: a rejected login rolls back, so
 * it can never delete anything itself. A failed sweep is logged and retried at the next interval
 * rather than ending the loop.
 */
@Suppress("TooGenericExceptionCaught")
internal fun Application.scheduleChallengeCleanup(auth: AuthFeature) {
    launch {
        while (isActive) {
            delay(CHALLENGE_CLEANUP_INTERVAL.toMillis())
            try {
                auth.cleanupExpiredChallenges()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                log.error("Expired challenge cleanup failed", error)
            }
        }
    }
}

private val CHALLENGE_CLEANUP_INTERVAL: Duration = Duration.ofMinutes(5)
