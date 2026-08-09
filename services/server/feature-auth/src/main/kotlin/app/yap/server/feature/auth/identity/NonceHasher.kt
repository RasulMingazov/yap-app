package app.yap.server.feature.auth.identity

/** Hashes a raw nonce the same way challenge issuance did, so the two values can be compared. */
internal fun interface NonceHasher {

    fun hash(value: String): String
}
