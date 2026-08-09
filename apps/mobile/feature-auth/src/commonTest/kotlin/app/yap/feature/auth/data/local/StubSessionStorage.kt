package app.yap.feature.auth.data.local

import io.github.rasulmingazov.stubcall.StubCall0
import io.github.rasulmingazov.stubcall.StubCall1

internal class StubSessionStorage(
    stored: SessionLocal? = null,
) : SessionStorage {

    val clearCall = StubCall0.unit()
    val readCall = StubCall0.returns<SessionLocal?>(stored)
    val writeCall = StubCall1.unit<SessionLocal>()

    override suspend fun clear() {
        clearCall.invoke()
        readCall.returns(null)
    }

    override suspend fun read(): SessionLocal? = readCall.invoke()

    override suspend fun write(session: SessionLocal) {
        writeCall.invoke(session)
        readCall.returns(session)
    }
}
