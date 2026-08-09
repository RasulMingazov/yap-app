package app.yap.feature.auth.data.mapper

import app.yap.contract.auth.SessionDto
import app.yap.feature.auth.data.local.SessionLocal
import app.yap.feature.auth.domain.entity.AccountId
import app.yap.feature.auth.domain.entity.Session

internal fun SessionLocal.toDomain(): Session = Session(accountId = AccountId(accountId))

internal fun SessionDto.toLocal(): SessionLocal = SessionLocal(
    accessToken = accessToken,
    accessTokenExpiresAtEpochSeconds = accessTokenExpiresAtEpochSeconds,
    accountId = accountId,
    refreshToken = refreshToken,
)
