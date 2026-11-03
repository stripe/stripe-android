package com.stripe.android.checkout

import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.repositories.CheckoutClientPollResponse
import com.stripe.android.paymentsheet.repositories.CheckoutClientPollResponse.ConfirmationStatus
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Determines the final Checkout Session state after the server accepted a confirmation and any next action completed.
 */
internal fun interface CheckoutConfirmationFinalizer {
    suspend fun finalize(response: CheckoutSessionResponse): Result<CheckoutSessionResponse>
}

internal class DefaultCheckoutConfirmationFinalizer internal constructor(
    private val poll: suspend (sessionId: String, clientSecret: String) -> Result<CheckoutClientPollResponse>,
    private val errorReporter: ErrorReporter,
) : CheckoutConfirmationFinalizer {

    @Inject
    internal constructor(
        checkoutSessionRepository: CheckoutSessionRepository,
        errorReporter: ErrorReporter,
    ) : this(
        poll = checkoutSessionRepository::poll,
        errorReporter = errorReporter,
    )

    override suspend fun finalize(response: CheckoutSessionResponse): Result<CheckoutSessionResponse> {
        if (response.status == CheckoutSessionResponse.Status.COMPLETE) {
            return Result.success(response)
        }

        val pollResponse = pollUntilTerminal(response)
        return when (pollResponse?.confirmationStatus) {
            // Optimistically succeed on timeout with the last server-reported state.
            null, ConfirmationStatus.PENDING, ConfirmationStatus.SUCCEEDED -> {
                Result.success(response.withPollResponse(pollResponse))
            }
            ConfirmationStatus.FAILED -> {
                Result.failure(IllegalStateException("Checkout Session confirmation failed."))
            }
            ConfirmationStatus.NOT_STARTED -> {
                errorReporter.report(ErrorReporter.UnexpectedErrorEvent.CHECKOUT_SESSION_CONFIRMATION_NOT_STARTED)
                Result.failure(
                    IllegalStateException(
                        "Checkout Session confirmation unexpectedly reported not_started after an accepted /confirm."
                    )
                )
            }
        }
    }

    private suspend fun pollUntilTerminal(response: CheckoutSessionResponse): CheckoutClientPollResponse? {
        var lastResponse: CheckoutClientPollResponse? = null
        withTimeoutOrNull(POLL_TIMEOUT) {
            while (true) {
                poll(response.id, response.clientSecret).onSuccess { lastResponse = it }
                val status = lastResponse?.confirmationStatus
                if (status != null && status != ConfirmationStatus.PENDING) break
                delay(POLL_INTERVAL)
            }
        }
        return lastResponse
    }

    internal companion object {
        val POLL_TIMEOUT = 30.seconds
        val POLL_INTERVAL = 500.milliseconds
    }
}
