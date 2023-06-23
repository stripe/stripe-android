package com.stripe.android.financialconnections.utils

import com.stripe.android.core.exception.StripeException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import java.net.HttpURLConnection
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.seconds

/**
 * Executes and returns the result of the given [block].
 * If the block execution fails, and [retryCondition] is met, the operation is retried.
 * Otherwise the resulting exception will be thrown.
 */
internal suspend fun <T> retryOnException(
    options: PollTimingOptions,
    retryCondition: suspend (Throwable) -> Boolean,
    block: suspend () -> T
): T = channelFlow {
    var remainingTimes = options.maxNumberOfRetries - 1
    while (!isClosedForSend) {
        delay(
            if (remainingTimes == options.maxNumberOfRetries - 1) {
                options.initialDelayMs
            } else {
                options.retryInterval
            }
        )
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

data class PollTimingOptions(
    val initialDelayMs: Long = 1.75.seconds.inWholeMilliseconds,
    val maxNumberOfRetries: Int = 180,
    val retryInterval: Long = 0.25.seconds.inWholeMilliseconds
)

/**
 * returns true if exception represents a [HttpURLConnection.HTTP_ACCEPTED] API response.
 */
internal val Throwable.shouldRetry: Boolean
    get() {
        val statusCode: Int? = (this as? StripeException)?.statusCode
        return statusCode == HttpURLConnection.HTTP_ACCEPTED
    }
