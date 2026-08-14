package app.yap.core.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
fun runViewModelTest(body: suspend TestScope.() -> Unit): TestResult = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        body()
    } finally {
        Dispatchers.resetMain()
    }
}
