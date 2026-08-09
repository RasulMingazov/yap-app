package app.yap.contract.auth

import kotlinx.serialization.Serializable

/**
 * An issued Yap session.
 *
 * [accountId] is the stable Yap account ID and the ownership key for learning progress: the same
 * identity from the same provider always resolves the same value.
 */
@Serializable
data class SessionDto(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAtEpochSeconds: Long,
    val accountId: String,
)
