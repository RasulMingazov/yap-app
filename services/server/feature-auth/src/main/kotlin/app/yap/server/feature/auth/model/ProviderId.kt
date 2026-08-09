package app.yap.server.feature.auth.model

/**
 * An identity-provider identifier exactly as it travels on the wire: a lowercase token such as
 * `google`. An unknown value is never an error here; it simply resolves to no registered verifier.
 */
@JvmInline
internal value class ProviderId(val value: String) {

    companion object {
        val Google = ProviderId("google")
    }
}
