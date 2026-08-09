package app.yap.feature.auth.data.local

import javax.crypto.SecretKey

internal interface SessionSecretKeyProvider {

    fun getOrCreateKey(): SecretKey
}
