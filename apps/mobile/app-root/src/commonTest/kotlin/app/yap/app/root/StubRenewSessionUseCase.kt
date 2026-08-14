package app.yap.app.root

import app.yap.feature.auth.api.usecase.RenewSessionUseCase
import io.github.rasulmingazov.stubcall.StubCall0
import kotlinx.coroutines.CompletableDeferred

internal class StubRenewSessionUseCase(
    private val gate: CompletableDeferred<Unit>? = null,
) : RenewSessionUseCase {

    val invokeCall = StubCall0.unit()

    override suspend fun invoke() {
        gate?.await()
        invokeCall.invoke()
    }
}
