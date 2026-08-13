package app.yap.core.common.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

abstract class BaseViewModel : ViewModel() {

    protected fun <T, R> StateFlow<T>.mapState(
        transform: (T) -> R,
    ): StateFlow<R> =
        map(transform).stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = transform(value),
        )
}
