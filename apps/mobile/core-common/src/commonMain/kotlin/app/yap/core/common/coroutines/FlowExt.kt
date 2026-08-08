package app.yap.core.common.coroutines

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flow

fun <T> Flow<T>.onFirst(action: suspend (T) -> Unit): Flow<T> =
    flow {
        collectIndexed { index, value ->
            if (index == 0) {
                action(value)
            }

            emit(value)
        }
    }
