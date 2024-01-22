package com.stripe.android.financialconnections.utils

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import java.net.HttpURLConnection
import kotlin.time.Duration.Companion.seconds

/**
 * Executes and returns the result of the given [block].
 * If the block execution fails, and [retryCondition] is met, the operation is retried.
 * Otherwise the resulting exception will be thrown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
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
                    remainingTimes == 0 -> throw PollingReachedMaxRetriesException(options)
                    retryCondition(exception).not() -> throw exception
                }
            },
            onSuccess = { send(it) }
        )
        remainingTimes--
    }
}.first()

internal data class PollTimingOptions(
    val initialDelayMs: Long = 1.75.seconds.inWholeMilliseconds,
    val maxNumberOfRetries: Int = 180,
    val retryInterval: Long = 0.25.seconds.inWholeMilliseconds
)

/**
 * Thrown when polling has reached the max number of retries.
 */
internal class PollingReachedMaxRetriesException(
    pollingOptions: PollTimingOptions
) : StripeException(
    message = "reached max number of retries ${pollingOptions.maxNumberOfRetries}.",
    statusCode = HttpURLConnection.HTTP_ACCEPTED
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String = "pollingReachedMaxRetriesError"
}

/**
 * returns true if exception represents a [HttpURLConnection.HTTP_ACCEPTED] API response.
 */
internal val Throwable.shouldRetry: Boolean
    get() {
        val statusCode: Int? = (this as? StripeException)?.statusCode
        return statusCode == HttpURLConnection.HTTP_ACCEPTED
    }
