package app.yap.feature.auth.data.local

import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal class StubSessionSecretKeyProvider(
    private val key: SecretKey = generateAesKey(),
) : SessionSecretKeyProvider {

    override fun getOrCreateKey(): SecretKey = key
}

private const val KEY_SIZE_BITS = 256

private fun generateAesKey(): SecretKey =
    KeyGenerator.getInstance("AES").apply { init(KEY_SIZE_BITS) }.generateKey()
