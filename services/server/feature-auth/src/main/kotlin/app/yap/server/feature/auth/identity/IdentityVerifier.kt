package app.yap.server.feature.auth.identity

import app.yap.server.feature.auth.model.LoginCredential
import app.yap.server.feature.auth.model.ProviderId
import app.yap.server.feature.auth.model.VerifiedIdentity

/** Turns a provider credential into the identity it proves. */
internal interface IdentityVerifier {

    val providerId: ProviderId

    /** Whether this provider accepts the PKCE authorization-code credential shape. */
    val supportsAuthorizationCode: Boolean

    /**
     * Verifies [credential] against the provider and the challenge it is bound to. [nonceHash] is
     * the stored hash of the issued nonce; the raw nonce is never accepted back from a client.
     *
     * Throws an `AuthFailureException` when the credential does not prove an identity.
     */
    suspend fun verify(credential: LoginCredential, nonceHash: String?): VerifiedIdentity
}
