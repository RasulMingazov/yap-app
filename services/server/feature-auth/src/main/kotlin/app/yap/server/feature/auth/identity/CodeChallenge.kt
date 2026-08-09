package app.yap.server.feature.auth.identity

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

/** The RFC 7636 `S256` transformation, the only code-challenge method this server accepts. */
internal object CodeChallenge {

    const val METHOD = "S256"

    fun s256(codeVerifier: String): String = MessageDigest.getInstance("SHA-256")
        .digest(codeVerifier.toByteArray(StandardCharsets.US_ASCII))
        .let(Base64.getUrlEncoder().withoutPadding()::encodeToString)
}
