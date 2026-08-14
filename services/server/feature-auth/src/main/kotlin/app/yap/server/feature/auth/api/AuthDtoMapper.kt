package app.yap.server.feature.auth.api

import app.yap.contract.auth.SessionDto
import app.yap.server.feature.auth.model.AuthenticatedSession

internal fun AuthenticatedSession.toDto(): SessionDto = SessionDto(
    accessToken = accessToken,
    refreshToken = refreshToken,
    accessTokenExpiresAtEpochSeconds = accessTokenExpiresAtEpochSeconds,
    refreshTokenExpiresAtEpochSeconds = refreshTokenExpiresAtEpochSeconds,
)
