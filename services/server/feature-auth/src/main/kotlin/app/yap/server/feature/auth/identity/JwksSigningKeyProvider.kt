package app.yap.server.feature.auth.identity

import com.auth0.jwk.JwkProvider
import java.security.interfaces.RSAPublicKey

/** Reads signing keys from a provider's published JWKS endpoint. */
internal class JwksSigningKeyProvider(private val jwkProvider: JwkProvider) : SigningKeyProvider {

    override fun publicKey(keyId: String?): RSAPublicKey =
        jwkProvider.get(keyId).publicKey as RSAPublicKey
}
