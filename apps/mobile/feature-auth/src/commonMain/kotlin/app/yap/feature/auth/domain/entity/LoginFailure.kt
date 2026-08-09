package app.yap.feature.auth.domain.entity

/**
 * Coarse, non-secret reason a login attempt could not complete. It never carries provider,
 * protocol, or credential detail (R-072, AC-025, AC-030).
 */
internal enum class LoginFailure {
    Configuration,
    Connectivity,
    Provider,
}
