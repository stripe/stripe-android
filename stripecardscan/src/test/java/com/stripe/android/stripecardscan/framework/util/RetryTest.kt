package com.stripe.android.stripecardscan.framework.util

import androidx.test.filters.SmallTest
import com.stripe.android.camera.framework.time.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RetryTest {

    @Test
    @SmallTest
    @ExperimentalCoroutinesApi
    fun `Retry succeeds if the first attempt succeeds`() = runTest {
        var executions = 0

        assertEquals(
            1,
            retry({ _, _ -> 1.milliseconds }) {
                executions++
                1
            }
        )
        assertEquals(1, executions)
    }

    @Test
    @SmallTest
    @ExperimentalCoroutinesApi
    fun `Retry succeeds if the second execution succeeds`() = runTest {
        var executions = 0

        assertEquals(
            1,
            retry({ _, _ -> 1.milliseconds }) {
                executions++
                if (executions == 2) {
                    1
                } else {
                    throw RuntimeException()
                }
            }
        )
        assertEquals(2, executions)
    }

    @Test
    @SmallTest
    @ExperimentalCoroutinesApi
    fun `Retry fails if all attempts fail`() = runTest {
        var executions = 0

        assertFailsWith<RuntimeException> {
            retry<Int>({ _, _ -> 1.milliseconds }) {
                executions++
                throw RuntimeException()
            }
        }
        assertEquals(3, executions)
    }

    @Test
    @SmallTest
    @ExperimentalCoroutinesApi
    fun `Retry does not retry excluded exception types`() = runTest {
        var executions = 0

        assertFailsWith<RuntimeException> {
            retry<Int>(
                retryDelayFunction = { _, _ -> 1.milliseconds },
                excluding = listOf(RuntimeException::class.java)
            ) {
                executions++
                throw RuntimeException()
            }
        }
        assertEquals(1, executions)
    }
}
