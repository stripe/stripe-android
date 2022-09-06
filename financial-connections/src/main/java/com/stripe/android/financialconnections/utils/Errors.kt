package com.stripe.android.financialconnections.utils

import com.stripe.android.core.exception.StripeException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
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
    block: suspend () -> T,
): T = channelFlow {
    while (!isClosedForSend) {
        delay(delayMilliseconds)
        val either = runCatching { block() }
        either.fold(onFailure = { exception ->
            if (retryCondition(exception)) {
                delay(delayMilliseconds)
            } else {
                throw exception
            }
        }, onSuccess = {
            send(it)
        })
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
