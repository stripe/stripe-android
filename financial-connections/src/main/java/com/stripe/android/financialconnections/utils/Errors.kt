package com.stripe.android.financialconnections.utils

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException
import kotlinx.coroutines.delay
import java.net.HttpURLConnection
import kotlin.time.Duration.Companion.seconds

/**
 * Executes and returns the result of the given [action].
 * If the block execution fails, and [retryCondition] is met, the operation is retried.
 * Otherwise the resulting exception will be thrown.
 */
internal suspend fun <T> retryOnException(
    options: PollTimingOptions,
    retryCondition: suspend (Throwable) -> Boolean,
    action: suspend () -> T
): T {
    val attempts = options.attempts
    var attempt = 1
    var result: T? = null

    while (attempt <= attempts && result == null) {
        if (attempt == 1) {
            delay(options.initialDelayMs)
        } else {
            delay(options.retryInterval)
        }

        runCatching {
            action()
        }.fold(
            onFailure = { exception ->
                if (!retryCondition(exception)) {
                    throw exception
                }
            },
            onSuccess = {
                result = it
            }
        )

        attempt += 1
    }

    return result ?: throw PollingReachedMaxRetriesException(options)
}

internal data class PollTimingOptions(
    val initialDelayMs: Long = 1.75.seconds.inWholeMilliseconds,
    val attempts: Int = 180,
    val retryInterval: Long = 0.25.seconds.inWholeMilliseconds
)

/**
 * Thrown when polling has reached the max number of retries.
 */
internal class PollingReachedMaxRetriesException(
    pollingOptions: PollTimingOptions
) : StripeException(
    message = "reached max number of attempts ${pollingOptions.attempts}.",
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
