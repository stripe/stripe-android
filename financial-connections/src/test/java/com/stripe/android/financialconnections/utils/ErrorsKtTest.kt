package com.stripe.android.financialconnections.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.net.HttpURLConnection

@OptIn(ExperimentalCoroutinesApi::class)
internal class ErrorsKtTest {

    @Test
    fun `should throw timeout if reaches max times`() = runTest {
        val testResult = kotlin.runCatching {
            retryOnException(
                PollTimingOptions(
                    maxNumberOfRetries = 5,
                    initialDelayMs = 0,
                    retryInterval = 1000
                ),
                retryCondition = { exception -> exception.shouldRetry }
            ) {
                throw retryException()
            }
        }
        assertThat(testResult.exceptionOrNull()!!).isInstanceOf(PollingReachedMaxRetriesException::class.java)
    }

    private fun retryException() = APIException(statusCode = HttpURLConnection.HTTP_ACCEPTED)

    @Test
    fun `should emit once if block always succeeds`() = runTest {
        var counter = 0
        val result = retryOnException(
            PollTimingOptions(
                maxNumberOfRetries = 5,
                initialDelayMs = 0,
                retryInterval = 1000
            ),
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
            PollTimingOptions(
                initialDelayMs = 0,
                retryInterval = 1000
            ),
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
                PollTimingOptions(
                    maxNumberOfRetries = 2,
                    initialDelayMs = 0,
                    retryInterval = 1000
                ),
                retryCondition = { exception -> exception.shouldRetry }
            ) {
                counter++
                if (counter == 3) true else throw retryException()
            }
        }
        assertThat(testResult.exceptionOrNull()!!).isInstanceOf(PollingReachedMaxRetriesException::class.java)
    }
}
