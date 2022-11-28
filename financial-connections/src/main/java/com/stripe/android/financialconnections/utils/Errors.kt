package com.stripe.android.financialconnections.utils

import com.stripe.android.core.exception.StripeException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import java.net.HttpURLConnection
import java.util.concurrent.TimeoutException

/**
 * Executes and returns the result of the given [block].
 * If the block execution fails, and [retryCondition] is met, the operation is retried.
 * Otherwise the resulting exception will be thrown.
 */
internal suspend fun <T> retryOnException(
    times: Int = Int.MAX_VALUE,
    initialDelay: Long = 0,
    delayMilliseconds: Long = 100,
    retryCondition: suspend (Throwable) -> Boolean,
    block: suspend () -> T
): T = channelFlow {
    var remainingTimes = times - 1
    while (!isClosedForSend) {
        delay(if (remainingTimes == times - 1) initialDelay else delayMilliseconds)
        val either = runCatching { block() }
        either.fold(
            onFailure = { exception ->
                when {
                    remainingTimes == 0 -> throw TimeoutException("reached max number of retries")
                    retryCondition(exception).not() -> throw exception
                }
            },
            onSuccess = { send(it) }
        )
        remainingTimes--
    }
}.first()

/**
 * returns true if exception represents a [HttpURLConnection.HTTP_ACCEPTED] API response.
 */
internal val Throwable.shouldRetry: Boolean
    get() {
        val statusCode: Int? = (this as? StripeException)?.statusCode
        return statusCode == HttpURLConnection.HTTP_ACCEPTED
    }
