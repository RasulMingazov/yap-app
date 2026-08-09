package app.yap.feature.auth.data.mapper

import app.yap.contract.auth.SessionDto
import app.yap.feature.auth.data.local.SessionDb
import app.yap.feature.auth.domain.entity.AccountId
import app.yap.feature.auth.domain.entity.Session

internal fun SessionDb.toDomain(): Session = Session(accountId = AccountId(accountId))

internal fun SessionDto.toDb(): SessionDb = SessionDb(
    accessToken = accessToken,
    accessTokenExpiresAtEpochSeconds = accessTokenExpiresAtEpochSeconds,
    accountId = accountId,
    refreshToken = refreshToken,
)
