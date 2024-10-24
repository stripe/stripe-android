package com.stripe.android.common.coroutines

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class SingleKtxTest {
    @Test
    fun `On converting 'StateFlow' to 'Single' should complete if it has a value`() = runTest {
        val flow = MutableStateFlow(value = 0)

        assertThat(flow.asSingle().await()).isEqualTo(0)
    }

    @Test
    fun `On converting 'StateFlow' to 'Single' should wait until value is set`() = runTest {
        val countDownLatch = CountDownLatch(1)

        val flow = MutableStateFlow<Int?>(null)
        val single = flow.asSingle()

        launch {
            assertThat(single.await()).isEqualTo(0)
            countDownLatch.countDown()
        }

        delay(50)

        flow.value = 0

        countDownLatch.await(5, TimeUnit.SECONDS)
    }

    @Test
    fun `On await with timeout, should return value if set`() = runTest {
        val flow = MutableStateFlow(value = 0)
        val single = flow.asSingle()

        val result = single.awaitWithTimeout(
            timeout = 1.seconds,
            timeoutMessage = {
                "No value was found!"
            }
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(0)
    }

    @Test
    fun `On await with timeout, should fail if timeout message if a value is not set`() = runTest {
        val flow = MutableStateFlow<Int?>(value = null)
        val single = flow.asSingle()

        val result = single.awaitWithTimeout(
            timeout = 1.seconds,
            timeoutMessage = {
                "No value was found!"
            }
        )

        assertThat(result.isFailure).isTrue()

        val exception = result.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception?.message).isEqualTo("No value was found!")
    }
}
