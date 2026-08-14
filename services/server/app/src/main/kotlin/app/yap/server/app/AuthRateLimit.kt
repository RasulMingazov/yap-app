package app.yap.server.app

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import kotlin.time.Duration.Companion.minutes

internal val AUTH_RATE_LIMIT_NAME = RateLimitName("auth")

internal fun Application.installAuthRateLimit(
    requestsPerMinute: Int,
    trustProxyHeaders: Boolean,
) {
    if (trustProxyHeaders) {
        install(XForwardedHeaders)
    }

    install(RateLimit) {
        register(AUTH_RATE_LIMIT_NAME) {
            rateLimiter(limit = requestsPerMinute, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteHost }
        }
    }
}
