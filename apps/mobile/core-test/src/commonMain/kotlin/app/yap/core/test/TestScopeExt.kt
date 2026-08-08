package app.yap.core.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.testDispatchers(): TestDispatchers =
    TestDispatchers(main = StandardTestDispatcher(testScheduler))
