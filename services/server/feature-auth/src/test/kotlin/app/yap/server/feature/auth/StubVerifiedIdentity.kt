package app.yap.server.feature.auth

import app.yap.server.feature.auth.model.ProviderId
import app.yap.server.feature.auth.model.VerifiedIdentity

internal object StubVerifiedIdentity {

    const val EMAIL = "user@example.com"
    const val SUBJECT = "google-subject"

    fun stubVerifiedIdentity(
        email: String? = EMAIL,
        isEmailVerified: Boolean? = true,
        provider: ProviderId = ProviderId.Google,
        subject: String = SUBJECT,
    ): VerifiedIdentity = VerifiedIdentity(
        email = email,
        isEmailVerified = isEmailVerified,
        provider = provider,
        subject = subject,
    )
}
