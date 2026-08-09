package app.yap.feature.auth.data.mapper

import app.yap.contract.auth.LoginChallengeDto
import app.yap.feature.auth.data.identity.LoginChallenge

internal fun LoginChallengeDto.toChallenge(): LoginChallenge = LoginChallenge(
    challengeId = challengeId,
    expiresAtEpochSeconds = expiresAtEpochSeconds,
    nonce = nonce,
)
