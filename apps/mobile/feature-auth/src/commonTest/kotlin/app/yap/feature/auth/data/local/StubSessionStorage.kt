package app.yap.feature.auth.data.local

import io.github.rasulmingazov.stubcall.StubCall0
import io.github.rasulmingazov.stubcall.StubCall1

internal class StubSessionStorage(
    stored: SessionDb? = null,
) : SessionStorage {

    val clearCall = StubCall0.unit()
    val readCall = StubCall0.returns<SessionDb?>(stored)
    val writeCall = StubCall1.unit<SessionDb>()

    override suspend fun clear() {
        clearCall.invoke()
        readCall.returns(null)
    }

    override suspend fun read(): SessionDb? = readCall.invoke()

    override suspend fun write(session: SessionDb) {
        writeCall.invoke(session)
        readCall.returns(session)
    }
}
