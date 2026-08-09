package app.yap.feature.auth.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class HttpFailureKindTest {

    @Test
    fun `GIVEN a successful status WHEN classifying it THEN the response is usable`() {
        assertNull(failureKindOf(statusCode = 200))
    }

    @Test
    fun `GIVEN a client error WHEN classifying it THEN the answer is definitive`() {
        assertEquals(
            expected = AuthApiFailureKind.Rejected,
            actual = failureKindOf(statusCode = 401),
        )
    }

    @Test
    fun `GIVEN a server error WHEN classifying it THEN the answer is transient`() {
        assertEquals(
            expected = AuthApiFailureKind.Unavailable,
            actual = failureKindOf(statusCode = 503),
        )
    }
}
