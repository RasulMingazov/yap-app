package app.yap.feature.auth.domain.entity

/**
 * Provider-neutral identifier. Presentation, navigation, and domain code carry it without
 * branching on a concrete provider (R-012, R-021, R-022).
 *
 * [id] is the stable lowercase identifier of the provider, used verbatim wherever a provider has to
 * be named outside Kotlin code.
 */
internal enum class LoginProviderId(val id: String) {
    Apple("apple"),
    Google("google"),
    Tid("tid"),
}
