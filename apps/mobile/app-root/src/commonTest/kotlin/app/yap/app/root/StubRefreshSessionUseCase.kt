package app.yap.app.root

import app.yap.feature.auth.api.usecase.RefreshSessionUseCase
import io.github.rasulmingazov.stubcall.StubCall0
import kotlinx.coroutines.CompletableDeferred

internal class StubRefreshSessionUseCase(
    private val gate: CompletableDeferred<Unit>? = null,
) : RefreshSessionUseCase {

    val invokeCall = StubCall0.unit()

    override suspend fun invoke() {
        gate?.await()
        invokeCall.invoke()
    }
}
