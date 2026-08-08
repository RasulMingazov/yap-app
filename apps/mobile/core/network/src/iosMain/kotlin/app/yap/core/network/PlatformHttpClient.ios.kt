package app.yap.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData

internal actual fun platformHttpClientEngine(): HttpClientEngine = Darwin.create {
    configureSession {
        URLCache = null
        requestCachePolicy = NSURLRequestReloadIgnoringLocalCacheData
    }
    configureRequest {
        setAllowsCellularAccess(true)
        setAssumesHTTP3Capable(false)
    }
}
