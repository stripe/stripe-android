package com.stripe.android.checkout

import android.content.Context
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.repositories.CheckoutClientPollResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Determines the final Checkout Session state after the server accepted a confirmation and any next action completed.
 */
internal fun interface CheckoutConfirmationFinalizer {
    suspend fun finalize(response: CheckoutSessionResponse): Result

    sealed interface Result {
        val response: CheckoutSessionResponse

        data class Completed(override val response: CheckoutSessionResponse) : Result

        data class Failed(
            override val response: CheckoutSessionResponse,
            val error: Throwable,
        ) : Result
    }
}

internal class DefaultCheckoutConfirmationFinalizer internal constructor(
    private val poller: CheckoutSessionPoller,
    private val poll: suspend (sessionId: String, clientSecret: String) -> Result<CheckoutClientPollResponse>,
    private val retrieve: suspend (sessionId: String, clientSecret: String) -> Result<CheckoutSessionResponse>,
    private val errorReporter: ErrorReporter,
    private val genericErrorMessage: String,
) : CheckoutConfirmationFinalizer {

    @Inject
    internal constructor(
        poller: CheckoutSessionPoller,
        checkoutSessionRepository: CheckoutSessionRepository,
        errorReporter: ErrorReporter,
        context: Context,
    ) : this(
        poller = poller,
        poll = checkoutSessionRepository::poll,
        retrieve = checkoutSessionRepository::retrieve,
        errorReporter = errorReporter,
        genericErrorMessage = context.getString(R.string.stripe_something_went_wrong),
    )

    override suspend fun finalize(
        response: CheckoutSessionResponse,
    ): CheckoutConfirmationFinalizer.Result {
        when (response.status) {
            CheckoutSessionResponse.Status.COMPLETE -> return CheckoutConfirmationFinalizer.Result.Completed(response)
            CheckoutSessionResponse.Status.EXPIRED -> return CheckoutConfirmationFinalizer.Result.Failed(
                response = response,
                error = IllegalStateException("Checkout Session unexpectedly expired after confirmation."),
            )
            CheckoutSessionResponse.Status.OPEN -> Unit
        }

        return when (val outcome = poller.poll { poll(response.id, response.clientSecret) }) {
            is CheckoutSessionPoller.Outcome.Succeeded -> CheckoutConfirmationFinalizer.Result.Completed(
                response = response.withPollResponse(outcome.response),
            )
            // Like iOS, optimistically succeed on timeout with the last server-reported state rather than deriving
            // the session state from the Intent.
            is CheckoutSessionPoller.Outcome.TimedOut -> CheckoutConfirmationFinalizer.Result.Completed(
                response = response.withPollResponse(outcome.lastResponse),
            )
            is CheckoutSessionPoller.Outcome.Failed -> failed(response.withPollResponse(outcome.response))
            is CheckoutSessionPoller.Outcome.NotStarted -> {
                errorReporter.report(ErrorReporter.UnexpectedErrorEvent.CHECKOUT_SESSION_CONFIRMATION_NOT_STARTED)
                CheckoutConfirmationFinalizer.Result.Failed(
                    response = response.withPollResponse(outcome.response),
                    error = IllegalStateException(
                        "Checkout Session confirmation unexpectedly reported not_started after an accepted /confirm."
                    ),
                )
            }
        }
    }

    /**
     * Poll responses have no error details, so retrieve the session to read them. Retrieving a completed session
     * fails, in which case a generic error is reported.
     */
    private suspend fun failed(
        response: CheckoutSessionResponse,
    ): CheckoutConfirmationFinalizer.Result.Failed {
        val latest = withTimeoutOrNull(RETRIEVE_TIMEOUT) {
            retrieve(response.id, response.clientSecret)
        }?.getOrNull()

        val error = if (latest != null) {
            latest.paymentResponse?.error?.let { InvalidRequestException(stripeError = it) }
                ?: genericError("checkoutSessionPaymentFailed")
        } else {
            genericError("pollFailedAndSessionRetrievalFailed")
        }

        return CheckoutConfirmationFinalizer.Result.Failed(
            response = latest ?: response,
            error = error,
        )
    }

    private fun genericError(analyticsValue: String) = LocalStripeException(
        displayMessage = genericErrorMessage,
        analyticsValue = analyticsValue,
    )

    private companion object {
        val RETRIEVE_TIMEOUT = 5.seconds
    }
}
