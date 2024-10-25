package com.stripe.android.common.coroutines

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class SingleKtxTest {
    @Test
    fun `On converting 'StateFlow' to 'Single' should complete if it has a value`() = runBlocking {
        val flow = MutableStateFlow(value = 0)

        assertThat(flow.asSingle().await()).isEqualTo(0)
    }

    @Test
    fun `On converting 'StateFlow' to 'Single' should wait until value is set`() = runBlocking {
        val flow = MutableStateFlow<Int?>(null)
        val single = flow.asSingle()

        val job = launch {
            assertThat(single.await()).isEqualTo(0)
        }

        delay(50)

        flow.value = 0

        job.join()
    }

    @Test
    fun `On await with timeout, should return value if set`() = runBlocking {
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
    fun `On await with timeout, should fail if timeout message if a value is not set`() = runBlocking {
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
