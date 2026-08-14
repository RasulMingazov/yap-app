package app.yap.feature.auth.data.identity

import kotlin.random.Random

private const val NONCE_BYTES = 32

internal fun interface NonceGenerator {

    fun generate(): String
}

internal class RandomNonceGenerator(
    private val random: Random = Random.Default,
) : NonceGenerator {

    override fun generate(): String = random.nextBytes(NONCE_BYTES)
        .joinToString(separator = "") { byte -> byte.toHexString() }

    private fun Byte.toHexString(): String {
        val value = toInt() and BYTE_MASK
        return HEX[value shr NIBBLE_BITS].toString() + HEX[value and NIBBLE_MASK]
    }

    private companion object {
        const val HEX = "0123456789abcdef"
        const val BYTE_MASK = 0xFF
        const val NIBBLE_BITS = 4
        const val NIBBLE_MASK = 0x0F
    }
}
