package app.yap.core.test

import app.yap.core.common.coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
object TestCoroutineDispatchers : CoroutineDispatchers {

    override val main: CoroutineDispatcher
        get() = UnconfinedTestDispatcher()
}
