package app.yap.server.feature.auth

import app.yap.server.feature.auth.model.AuthAccount
import java.time.Instant

internal object StubAuthAccount {

    const val ACCOUNT_ID = "11111111-1111-1111-1111-111111111111"

    fun stubAuthAccount(
        createdAt: Instant = StubAuth.NOW,
        id: String = ACCOUNT_ID,
    ): AuthAccount = AuthAccount(
        createdAt = createdAt,
        id = id,
    )
}
