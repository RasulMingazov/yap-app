package app.yap.contract.auth

import kotlinx.serialization.Serializable

/** Presents a refresh credential for rotation. */
@Serializable
data class RefreshRequestDto(
    val refreshToken: String,
)
