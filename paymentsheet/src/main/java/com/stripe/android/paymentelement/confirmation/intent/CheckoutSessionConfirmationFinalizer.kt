package com.stripe.android.paymentelement.confirmation.intent

import android.content.Context
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

internal class CheckoutSessionConfirmationFinalizer internal constructor(
    private val poller: CheckoutSessionPoller,
    private val retrieveSession: suspend (String) -> Result<CheckoutSessionResponse>,
    private val genericErrorMessage: String,
) {
    @Inject
    constructor(
        poller: DefaultCheckoutSessionPoller,
        repository: CheckoutSessionRepository,
        context: Context,
    ) : this(poller, repository::retrieve, context.getString(R.string.stripe_something_went_wrong))

    suspend fun finalize(
        response: CheckoutSessionResponse,
        intent: StripeIntent?,
    ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> {
        val outcome = when (response.status) {
            CheckoutSessionResponse.Status.OPEN -> poller.poll(response.id)
            CheckoutSessionResponse.Status.COMPLETE -> null
            CheckoutSessionResponse.Status.EXPIRED -> return fail(
                response,
                unexpectedError("Checkout Session unexpectedly expired after confirmation."),
            )
        }
        when (outcome) {
            CheckoutSessionPoller.Outcome.REQUIRES_PAYMENT_METHOD,
            CheckoutSessionPoller.Outcome.FAILED_ASYNC_PAYMENT,
            CheckoutSessionPoller.Outcome.INVALID_OR_EXPIRED -> {
                val latest = retrieveSession(response.id).getOrElse { return fail(response, it) }
                return fail(
                    latest,
                    if (outcome == CheckoutSessionPoller.Outcome.INVALID_OR_EXPIRED) {
                        unexpectedError("Checkout Session unexpectedly became invalid or expired during polling.")
                    } else {
                        paymentError(latest)
                    },
                )
            }
            CheckoutSessionPoller.Outcome.COMPLETED,
            CheckoutSessionPoller.Outcome.TIMED_OUT,
            null -> Unit
        }
        val merged = merge(response, intent, outcome == CheckoutSessionPoller.Outcome.COMPLETED)
        return ConfirmationDefinition.Action.Complete(
            intent = intent,
            metadata = metadata(merged),
            completedFullPaymentFlow = true,
        )
    }

    fun fail(
        response: CheckoutSessionResponse,
        error: Throwable,
    ): ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args> {
        if (error is CancellationException) throw error
        return ConfirmationDefinition.Action.Fail(
            cause = error,
            message = error.stripeErrorMessage(),
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            metadata = metadata(response),
        )
    }

    fun paymentError(response: CheckoutSessionResponse): Throwable {
        val paymentError = response.paymentIntent?.lastPaymentError
        val setupError = response.setupIntent?.lastSetupError
        return LocalStripeException(
            displayMessage = (if (paymentError != null) paymentError.message else setupError?.message)
                ?: genericErrorMessage,
            analyticsValue = "checkoutSessionPaymentError",
            errorCode = if (paymentError != null) paymentError.code else setupError?.code,
            declineCode = if (paymentError != null) paymentError.declineCode else setupError?.declineCode,
            type = if (paymentError != null) paymentError.type?.code else setupError?.type?.code,
        )
    }

    fun unexpectedError(message: String): Throwable = IllegalStateException(message)

    private fun merge(
        response: CheckoutSessionResponse,
        intent: StripeIntent?,
        didPollToCompletion: Boolean,
    ): CheckoutSessionResponse {
        val complete = when (intent) {
            is PaymentIntent ->
                intent.status == StripeIntent.Status.Succeeded || intent.status == StripeIntent.Status.Processing
            is SetupIntent -> intent.status == StripeIntent.Status.Succeeded
            null -> didPollToCompletion
        }
        return response.copy(
            status = if (complete) CheckoutSessionResponse.Status.COMPLETE else response.status,
            paymentStatus = if (intent is PaymentIntent && intent.status == StripeIntent.Status.Succeeded) {
                CheckoutSessionResponse.PaymentStatus.PAID
            } else {
                response.paymentStatus
            },
            paymentIntent = (intent as? PaymentIntent) ?: response.paymentIntent,
            setupIntent = (intent as? SetupIntent) ?: response.setupIntent,
            elementsSession = if (intent == null) {
                response.elementsSession
            } else {
                response.elementsSession?.copy(stripeIntent = intent)
            },
        )
    }

    companion object {
        fun metadata(response: CheckoutSessionResponse) = MutableConfirmationMetadata().apply {
            set(DeferredIntentConfirmationTypeKey, DeferredIntentConfirmationType.Server)
            set(CheckoutSessionResponseKey, response)
        }
    }
}
