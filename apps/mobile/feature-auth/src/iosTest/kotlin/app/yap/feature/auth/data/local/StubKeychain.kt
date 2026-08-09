package app.yap.feature.auth.data.local

import io.github.rasulmingazov.stubcall.StubCall1
import io.github.rasulmingazov.stubcall.StubCall2

internal class StubKeychain(
    stored: String? = null,
) : Keychain {

    val deleteCall = StubCall1.unit<KeychainQuery>()
    val readCall = StubCall1.returns<KeychainQuery, String?>(stored)
    val writeCall = StubCall2.unit<KeychainQuery, String>()

    override fun delete(query: KeychainQuery) {
        deleteCall.invoke(query)
        readCall.returns(null)
    }

    override fun read(query: KeychainQuery): String? = readCall.invoke(query)

    override fun write(query: KeychainQuery, value: String) {
        writeCall.invoke(query, value)
        readCall.returns(value)
    }
}
