package app.yap.feature.auth.domain.entity

/**
 * Provider-neutral identifier. Presentation, navigation, and domain code carry it without
 * branching on a concrete provider (R-012, R-021, R-022).
 */
internal enum class LoginProviderId {
    Apple,
    Google,
    Tid,
}
