package app.yap.server.feature.auth.model

/**
 * Every authentication outcome a client may observe, with its wire [code].
 *
 * Each challenge problem — missing, expired, provider-mismatched, or proof-mismatched — collapses
 * into [ChallengeInvalid], so a response never discloses challenge state, and a never-issued
 * challenge is indistinguishable from an already-consumed one.
 *
 * A provider whose configuration is absent is not registered and fails as [ProviderUnavailable].
 * The server has no "coming soon" outcome: provider availability is a client-side concern.
 */
internal enum class AuthFailure(val code: String) {
    ChallengeInvalid("challenge_invalid"),
    InvalidRequest("invalid_request"),
    ProviderUnavailable("provider_unavailable"),
}
