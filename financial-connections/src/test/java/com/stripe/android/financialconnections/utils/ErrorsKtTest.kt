package com.stripe.android.financialconnections.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.net.HttpURLConnection
import java.util.concurrent.TimeoutException

internal class ErrorsKtTest {

    @Test
    fun `should throw timeout if reaches max times`() = runTest {
        val testResult = kotlin.runCatching {
            retryOnException(
                times = 5,
                delayMilliseconds = 1_000,
                retryCondition = { exception -> exception.shouldRetry }
            ) {
                throw retryException()
            }
        }
        assertThat(testResult.exceptionOrNull()!!).isInstanceOf(TimeoutException::class.java)
    }

    private fun retryException() = APIException(statusCode = HttpURLConnection.HTTP_ACCEPTED)

    @Test
    fun `should emit once if block always succeeds`() = runTest {
        var counter = 0
        val result = retryOnException(
            times = 5,
            delayMilliseconds = 1_000,
            retryCondition = { exception -> exception.shouldRetry }
        ) {
            counter++
            true
        }
        assertThat(counter).isEqualTo(1)
        assertThat(result).isTrue()
    }

    @Test
    fun `should retry and emit once when succeeds`() = runTest {
        var counter = 0
        val result = retryOnException(
            delayMilliseconds = 1_000,
            retryCondition = { exception -> exception.shouldRetry }
        ) {
            counter++
            if (counter == 3) true else throw retryException()
        }
        assertThat(counter).isEqualTo(3)
        assertThat(result).isTrue()
    }

    @Test
    fun `should retry and fail with timeout if exceeds retries`() = runTest {
        val testResult = kotlin.runCatching {
            var counter = 0
            retryOnException(
                times = 2,
                delayMilliseconds = 1_000,
                retryCondition = { exception -> exception.shouldRetry }
            ) {
                counter++
                if (counter == 3) true else throw retryException()
            }
        }
        assertThat(testResult.exceptionOrNull()!!).isInstanceOf(TimeoutException::class.java)
    }
}
