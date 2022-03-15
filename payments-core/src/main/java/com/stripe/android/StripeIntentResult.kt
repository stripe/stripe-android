package com.stripe.android

import androidx.annotation.IntDef
import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.StripeIntent

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

        return when (stripeIntentStatus) {
            StripeIntent.Status.RequiresAction -> {
                if (isNextActionSuccessState(intent)) {
                    Outcome.SUCCEEDED
                } else {
                    Outcome.CANCELED
                }
            }
            StripeIntent.Status.Canceled -> {
                Outcome.CANCELED
            }
            StripeIntent.Status.RequiresPaymentMethod -> {
                Outcome.FAILED
            }
            StripeIntent.Status.Succeeded,
            StripeIntent.Status.RequiresCapture,
            StripeIntent.Status.RequiresConfirmation -> {
                Outcome.SUCCEEDED
            }
            StripeIntent.Status.Processing -> {
                if (intent.paymentMethod?.type?.hasDelayedSettlement() == true) {
                    Outcome.SUCCEEDED
                } else {
                    Outcome.UNKNOWN
                }
            }
            else -> {
                Outcome.UNKNOWN
            }
        }
    }

    /**
     * Check if the [nextAction] is expected state after a successful on-session transaction
     * e.g. for voucher-based payment methods like OXXO that require out-of-band payment and
     * ACHv2 payments which requires verification of the customers bank details before
     * confirming payment.
     */
    private fun isNextActionSuccessState(nextAction: StripeIntent): Boolean {
        return when (nextAction.nextActionType) {
            StripeIntent.NextActionType.RedirectToUrl,
            StripeIntent.NextActionType.UseStripeSdk,
            StripeIntent.NextActionType.AlipayRedirect,
            StripeIntent.NextActionType.BlikAuthorize,
            StripeIntent.NextActionType.WeChatPayRedirect,
            null -> {
                false
            }
            StripeIntent.NextActionType.DisplayOxxoDetails,
            StripeIntent.NextActionType.VerifyWithMicroDeposits -> {
                true
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
