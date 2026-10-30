package com.stripe.android.checkout

import com.stripe.android.core.exception.StripeException
import com.stripe.android.paymentsheet.repositories.CheckoutClientPollResponse
import com.stripe.android.paymentsheet.repositories.CheckoutClientPollResponse.ConfirmationStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Polls a Checkout Session after an accepted confirmation until its confirmation status is terminal.
 */
internal class CheckoutSessionPoller internal constructor(
    private val timeSource: TimeSource,
) {
    @Inject
    constructor() : this(TimeSource.Monotonic)

    sealed interface Outcome {
        data class Succeeded(val response: CheckoutClientPollResponse) : Outcome
        data class Failed(val response: CheckoutClientPollResponse) : Outcome
        data class NotStarted(val response: CheckoutClientPollResponse) : Outcome
        data class TimedOut(val lastResponse: CheckoutClientPollResponse?) : Outcome
    }

    suspend fun poll(
        request: suspend () -> Result<CheckoutClientPollResponse>,
    ): Outcome {
        val start = timeSource.markNow()
        var minimumInterval = MINIMUM_INTERVAL
        var rateLimitCount = 0
        var lastPollStart: Duration? = null
        var lastResponse: CheckoutClientPollResponse? = null

        while (start.elapsedNow() < TIMEOUT) {
            if (lastPollStart != null) {
                val elapsed = start.elapsedNow()
                val wait = (minimumInterval - (elapsed - lastPollStart)).coerceAtLeast(Duration.ZERO)
                val remaining = (TIMEOUT - elapsed).coerceAtLeast(Duration.ZERO)
                if (wait >= remaining) {
                    return Outcome.TimedOut(lastResponse)
                }
                delay(wait)
            }

            val remaining = TIMEOUT - start.elapsedNow()
            if (remaining <= Duration.ZERO) {
                return Outcome.TimedOut(lastResponse)
            }

            lastPollStart = start.elapsedNow()
            val result = withTimeoutOrNull(remaining) { request() }
            val response = result?.getOrNull()

            if (response == null) {
                if (result?.exceptionOrNull()?.isRateLimit() == true) {
                    minimumInterval = minOf(
                        RATE_LIMIT_MINIMUM_INTERVAL * 2.0.pow(rateLimitCount),
                        RATE_LIMIT_MAXIMUM_INTERVAL,
                    )
                    rateLimitCount++
                }
                continue
            }

            lastResponse = response
            minimumInterval = MINIMUM_INTERVAL
            rateLimitCount = 0

            when (response.confirmationStatus) {
                ConfirmationStatus.PENDING -> Unit
                ConfirmationStatus.SUCCEEDED -> return Outcome.Succeeded(response)
                ConfirmationStatus.FAILED -> return Outcome.Failed(response)
                ConfirmationStatus.NOT_STARTED -> return Outcome.NotStarted(response)
            }
        }

        return Outcome.TimedOut(lastResponse)
    }

    private fun Throwable.isRateLimit(): Boolean {
        return (this as? StripeException)?.statusCode == HTTP_TOO_MANY_REQUESTS
    }

    internal companion object {
        val TIMEOUT = 30.seconds
        val MINIMUM_INTERVAL = 500.milliseconds
        val RATE_LIMIT_MINIMUM_INTERVAL = 2.seconds
        val RATE_LIMIT_MAXIMUM_INTERVAL = 8.seconds
        private const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
