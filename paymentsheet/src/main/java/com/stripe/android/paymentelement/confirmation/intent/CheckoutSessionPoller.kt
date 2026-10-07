package com.stripe.android.paymentelement.confirmation.intent

import android.os.SystemClock
import com.stripe.android.core.exception.StripeException
import com.stripe.android.paymentsheet.repositories.CheckoutSessionPollResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

internal fun interface CheckoutSessionPoller {
    suspend fun poll(sessionId: String): Outcome

    enum class Outcome { COMPLETED, TIMED_OUT, REQUIRES_PAYMENT_METHOD, FAILED_ASYNC_PAYMENT, INVALID_OR_EXPIRED }
}

internal class DefaultCheckoutSessionPoller internal constructor(
    private val request: suspend (String, Long) -> Result<CheckoutSessionPollResponse>,
    private val clock: () -> Long,
) : CheckoutSessionPoller {
    @Inject
    constructor(repository: CheckoutSessionRepository) : this(repository::poll, SystemClock::elapsedRealtime)

    override suspend fun poll(sessionId: String): CheckoutSessionPoller.Outcome {
        return withTimeoutOrNull(BUDGET_MILLIS) { pollUntilDeadline(sessionId) }
            ?: CheckoutSessionPoller.Outcome.TIMED_OUT
    }

    private suspend fun pollUntilDeadline(sessionId: String): CheckoutSessionPoller.Outcome {
        val deadline = clock() + BUDGET_MILLIS
        var interval = MIN_INTERVAL_MILLIS
        var nextRequestStart = clock()
        while (true) {
            currentCoroutineContext().ensureActive()
            val wait = nextRequestStart - clock()
            if (wait > 0) delay(wait.coerceAtMost((deadline - clock()).coerceAtLeast(0)))
            val remaining = deadline - clock()
            if (remaining <= 0) return CheckoutSessionPoller.Outcome.TIMED_OUT
            val startedAt = clock()
            val result = requestSafely(sessionId, remaining)
            currentCoroutineContext().ensureActive()
            if (clock() >= deadline) return CheckoutSessionPoller.Outcome.TIMED_OUT
            val response = result.getOrNull()
            if (response != null) {
                interval = MIN_INTERVAL_MILLIS
                outcome(response)?.let { return it }
            } else {
                interval = intervalAfterError(interval, result.exceptionOrNull())
            }
            nextRequestStart = startedAt + interval
        }
    }

    private suspend fun requestSafely(sessionId: String, remaining: Long): Result<CheckoutSessionPollResponse> = try {
        request(sessionId, remaining)
    } catch (error: CancellationException) {
        throw error
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        Result.failure(error)
    }

    private fun intervalAfterError(interval: Long, error: Throwable?): Long {
        if (error is CancellationException) throw error
        return if ((error as? StripeException)?.statusCode == TOO_MANY_REQUESTS) {
            if (interval == MIN_INTERVAL_MILLIS) {
                INITIAL_BACKOFF_MILLIS
            } else {
                (interval * 2).coerceAtMost(MAX_INTERVAL_MILLIS)
            }
        } else {
            interval
        }
    }

    private fun outcome(response: CheckoutSessionPollResponse): CheckoutSessionPoller.Outcome? {
        if (response.paymentObjectStatus == "requires_payment_method") {
            return CheckoutSessionPoller.Outcome.REQUIRES_PAYMENT_METHOD
        }
        return outcome(response.state)
    }

    private fun outcome(state: CheckoutSessionPollResponse.State): CheckoutSessionPoller.Outcome? = when (state) {
        CheckoutSessionPollResponse.State.ACTIVE,
        CheckoutSessionPollResponse.State.PROCESSING_SYNC_PAYMENT,
        CheckoutSessionPollResponse.State.PROCESSING_SUBSCRIPTION -> null
        CheckoutSessionPollResponse.State.SUCCEEDED,
        CheckoutSessionPollResponse.State.PROCESSING_ASYNC_PAYMENT,
        CheckoutSessionPollResponse.State.PENDING_ASYNC_CUSTOMER_ACTION -> CheckoutSessionPoller.Outcome.COMPLETED
        CheckoutSessionPollResponse.State.FAILED_ASYNC_PAYMENT -> CheckoutSessionPoller.Outcome.FAILED_ASYNC_PAYMENT
        CheckoutSessionPollResponse.State.INVALID,
        CheckoutSessionPollResponse.State.EXPIRED -> CheckoutSessionPoller.Outcome.INVALID_OR_EXPIRED
    }

    private companion object {
        const val TOO_MANY_REQUESTS = 429
        const val BUDGET_MILLIS = 30_000L
        const val MIN_INTERVAL_MILLIS = 500L
        const val INITIAL_BACKOFF_MILLIS = 2_000L
        const val MAX_INTERVAL_MILLIS = 8_000L
    }
}
