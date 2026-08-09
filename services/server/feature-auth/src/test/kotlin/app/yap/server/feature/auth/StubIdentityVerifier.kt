package app.yap.server.feature.auth

import app.yap.server.feature.auth.identity.IdentityVerifier
import app.yap.server.feature.auth.model.LoginCredential
import app.yap.server.feature.auth.model.ProviderId
import app.yap.server.feature.auth.model.VerifiedIdentity
import io.github.rasulmingazov.stubcall.StubCall2

internal class StubIdentityVerifier(
    identity: VerifiedIdentity = StubVerifiedIdentity.stubVerifiedIdentity(),
    override val providerId: ProviderId = ProviderId.Google,
    override val supportsAuthorizationCode: Boolean = true,
) : IdentityVerifier {

    val verifyCall = StubCall2.returns<LoginCredential, String?, VerifiedIdentity>(identity)

    override suspend fun verify(
        credential: LoginCredential,
        nonceHash: String?,
    ): VerifiedIdentity = verifyCall.invoke(credential, nonceHash)
}
