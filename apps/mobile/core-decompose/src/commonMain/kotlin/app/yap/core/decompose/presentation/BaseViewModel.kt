package app.yap.core.decompose.presentation

import app.yap.core.common.coroutines.CoroutineDispatchers
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Base for retained Decompose models. Registers itself as an [InstanceKeeper.Instance] so
 * [viewModelScope] cleanup runs automatically when the owning `instanceKeeper` destroys the
 * instance; subclasses do not declare `InstanceKeeper.Instance` or forward `onDestroy()`
 * themselves.
 */
abstract class BaseViewModel(
    coroutineDispatchers: CoroutineDispatchers,
) : InstanceKeeper.Instance {

    protected val viewModelScope: CoroutineScope = CoroutineScope(
        coroutineDispatchers.main + SupervisorJob(),
    )

    final override fun onDestroy() {
        onCleared()
        viewModelScope.cancel()
    }

    protected open fun onCleared() = Unit
}
