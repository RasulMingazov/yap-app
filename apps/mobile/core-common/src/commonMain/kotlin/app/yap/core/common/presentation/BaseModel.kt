package app.yap.core.common.presentation

import app.yap.core.common.coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

abstract class BaseModel(
    coroutineDispatchers: CoroutineDispatchers,
) {

    protected val modelScope: CoroutineScope = CoroutineScope(
        coroutineDispatchers.main + SupervisorJob(),
    )

    fun clear() {
        onCleared()
        modelScope.cancel()
    }

    protected fun <T, R> StateFlow<T>.mapState(
        transform: (T) -> R,
    ): StateFlow<R> =
        map(transform).stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = transform(value),
        )

    protected open fun onCleared() = Unit
}
