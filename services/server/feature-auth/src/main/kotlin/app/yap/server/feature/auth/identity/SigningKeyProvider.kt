package app.yap.server.feature.auth.identity

import java.security.interfaces.RSAPublicKey

/** Supplies the provider's published signing key for a token's `kid`. */
internal fun interface SigningKeyProvider {

    fun publicKey(keyId: String?): RSAPublicKey
}
