package app.yap.server.feature.auth.persistence

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

/**
 * Releases both attempts from one latch, so the database decides which of them wins instead of the
 * test's timing. Each attempt runs on its own thread and therefore on its own connection.
 */
internal fun <T> runConcurrently(first: suspend () -> T, second: suspend () -> T): List<T> {
    val executor = Executors.newFixedThreadPool(ATTEMPT_COUNT)
    val released = CountDownLatch(1)
    val attempts = listOf(first, second).map { attempt ->
        executor.submit<T> {
            released.await()
            runBlocking { attempt() }
        }
    }

    released.countDown()
    return attempts.map { attempt -> attempt.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        .also { executor.shutdownNow() }
}

private const val ATTEMPT_COUNT = 2
private const val TIMEOUT_SECONDS = 30L
