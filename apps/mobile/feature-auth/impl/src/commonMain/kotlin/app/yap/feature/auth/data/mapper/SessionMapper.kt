package app.yap.feature.auth.data.mapper

import app.yap.contract.auth.SessionDto
import app.yap.feature.auth.api.entity.AuthState
import app.yap.feature.auth.api.entity.UserId
import app.yap.feature.auth.data.local.SessionLocal
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val SUBJECT_CLAIM = "sub"
private const val PAYLOAD_SEGMENT = 1

internal fun SessionDto.toLocal(): SessionLocal = SessionLocal(
    accessToken = accessToken,
    refreshToken = refreshToken,
    accessTokenExpiresAtEpochSeconds = accessTokenExpiresAtEpochSeconds,
    refreshTokenExpiresAtEpochSeconds = refreshTokenExpiresAtEpochSeconds,
)

internal fun SessionLocal.toDomain(): AuthState {
    val userId = accessToken.subjectClaim() ?: return AuthState.LoggedOut
    return AuthState.LoggedIn(userId = UserId(userId))
}

@OptIn(ExperimentalEncodingApi::class)
private fun String.subjectClaim(): String? {
    val payload = split('.').getOrNull(PAYLOAD_SEGMENT) ?: return null
    return runCatching {
        val decoded = Base64.UrlSafe
            .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
            .decode(payload)
            .decodeToString()
        Json.parseToJsonElement(decoded).jsonObject[SUBJECT_CLAIM]?.jsonPrimitive?.contentOrNull
    }.getOrNull()?.takeIf(String::isNotBlank)
}
