package app.yap.server.app

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.plugins.origin
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.event.Level

private const val HEALTH_PATH = "/health"

/**
 * One line per handled request: method, path, status, origin and duration. The health probe is
 * left out — the platform polls it every few seconds and it would drown everything else. The
 * origin honours the proxy headers when [installAuthRateLimit] trusts them, so it is the same
 * address the rate limit keys on.
 */
internal fun Application.installRequestLogging() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path() != HEALTH_PATH }
        format { call ->
            val status = call.response.status()?.value ?: "-"
            "${call.request.httpMethod.value} ${call.request.path()} -> $status " +
                "from ${call.request.origin.remoteHost} in ${call.processingTimeMillis()}ms"
        }
    }
}
