package app.yap.core.test

import app.yap.core.common.coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher

class TestDispatchers(
    override val main: CoroutineDispatcher,
) : CoroutineDispatchers
