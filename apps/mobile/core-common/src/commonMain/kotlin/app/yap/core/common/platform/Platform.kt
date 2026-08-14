package app.yap.core.common.platform

enum class Platform {
    ANDROID,
    IOS,
}

expect fun currentPlatform(): Platform
