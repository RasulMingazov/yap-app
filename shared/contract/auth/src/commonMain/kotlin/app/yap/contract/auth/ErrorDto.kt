package app.yap.contract.auth

import kotlinx.serialization.Serializable

/**
 * A failed authentication response.
 *
 * [code] is one of `"challenge_invalid"`, `"invalid_request"`, or `"provider_unavailable"`. Every
 * challenge problem — missing, expired, provider-mismatched, or proof-mismatched — collapses into
 * the single opaque `"challenge_invalid"`, so the response reveals no challenge state.
 */
@Serializable
data class ErrorDto(
    val code: String,
    val message: String,
)
