package com.stripe.android.financialconnections.utils

import com.stripe.android.core.exception.StripeException
import kotlinx.coroutines.delay
import java.net.HttpURLConnection

/**
 * Executes and returns the result of the given [block].
 * If the block execution fails, and [retryCondition] is met, the operation is retried.
 * Otherwise the resulting exception will be thrown.
 */
internal suspend fun <T> retryOnException(
    times: Int = Int.MAX_VALUE,
    delayMilliseconds: Long = 100,
    retryCondition: suspend (Throwable) -> Boolean,
    block: suspend () -> T
): T {
    repeat(times - 1) {
        val result = runCatching { block() }
        result.exceptionOrNull()?.let { exception ->
            if (retryCondition(exception)) {
                delay(delayMilliseconds)
            } else {
                throw exception
            }
        } ?: requireNotNull(result.getOrNull())
    }
    return block()
}

/**
 * returns true if exception represents a [HttpURLConnection.HTTP_ACCEPTED] API response.
 */
internal val Throwable.shouldRetry: Boolean
    get() {
        val statusCode: Int? = (this as? StripeException)?.statusCode
        return statusCode == HttpURLConnection.HTTP_ACCEPTED
    }
