package com.stripe.android

import androidx.annotation.IntDef
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeModel

/**
 * A model representing the result of a [StripeIntent] confirmation or authentication attempt
 * via [Stripe.confirmPayment] or [Stripe.handleNextActionForPayment]
 *
 * [intent] is a [StripeIntent] retrieved after confirmation/authentication succeeded or failed.
 */
abstract class StripeIntentResult<out T : StripeIntent> internal constructor(
    @Outcome private val outcomeFromFlow: Int
) : StripeModel {
    abstract val intent: T
    abstract val failureMessage: String?

    @Outcome
    @get:Outcome
    val outcome: Int
        get() = determineOutcome(intent.status, outcomeFromFlow)

    @StripeIntentResult.Outcome
    private fun determineOutcome(
        stripeIntentStatus: StripeIntent.Status?,
        @StripeIntentResult.Outcome outcome: Int
    ): Int {
        if (outcome != Outcome.UNKNOWN) {
            return outcome
        }

        when (stripeIntentStatus) {
            StripeIntent.Status.RequiresAction, StripeIntent.Status.Canceled -> {
                return Outcome.CANCELED
            }
            StripeIntent.Status.RequiresPaymentMethod -> {
                return Outcome.FAILED
            }
            StripeIntent.Status.Succeeded,
            StripeIntent.Status.RequiresCapture,
            StripeIntent.Status.RequiresConfirmation -> {
                return Outcome.SUCCEEDED
            }
            StripeIntent.Status.Processing -> {
                return if (intent.paymentMethod?.type?.hasDelayedSettlement == true) {
                    Outcome.SUCCEEDED
                } else {
                    Outcome.UNKNOWN
                }
            }
            else -> {
                return Outcome.UNKNOWN
            }
        }
    }

    /**
     * Values that indicate the outcome of confirmation and payment authentication.
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(Outcome.UNKNOWN, Outcome.SUCCEEDED, Outcome.FAILED, Outcome.CANCELED, Outcome.TIMEDOUT)
    annotation class Outcome {
        companion object {
            const val UNKNOWN: Int = 0

            /**
             * Confirmation or payment authentication succeeded
             */
            const val SUCCEEDED: Int = 1

            /**
             * Confirm or payment authentication failed
             */
            const val FAILED: Int = 2

            /**
             * Payment authentication was canceled by the user
             */
            const val CANCELED: Int = 3

            /**
             * Payment authentication timed-out
             */
            const val TIMEDOUT: Int = 4
        }
    }
}
