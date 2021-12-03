package com.stripe.android.paymentsheet.model

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import javax.inject.Inject

/**
 * Validator for [PaymentIntent] or [SetupIntent] instances used in PaymentSheet.
 */
internal class StripeIntentValidator @Inject constructor() {
    @JvmSynthetic
    fun requireValid(
        stripeIntent: StripeIntent
    ): StripeIntent {
        when {
            stripeIntent is PaymentIntent &&
                stripeIntent.confirmationMethod != PaymentIntent.ConfirmationMethod.Automatic -> {
                error(
                    """
                        PaymentIntent with confirmation_method='automatic' is required.
                        The current PaymentIntent has confirmation_method '${stripeIntent.confirmationMethod}'.
                        See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-confirmation_method.
                    """.trimIndent()
                )
            }
            stripeIntent is PaymentIntent &&
                (
                    (stripeIntent.status == StripeIntent.Status.Canceled) ||
                        (stripeIntent.status == StripeIntent.Status.Succeeded) ||
                        (stripeIntent.status == StripeIntent.Status.RequiresCapture)
                    ) -> {
                error(
                    """
                        PaymentSheet received a PaymentIntent in a terminal state.
                        The current PaymentIntent has status '${stripeIntent.status}'.
                        See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-status.
                    """.trimIndent()
                )
            }
            stripeIntent is SetupIntent &&
                (
                    (stripeIntent.status == StripeIntent.Status.Canceled) ||
                        (stripeIntent.status == StripeIntent.Status.Succeeded)
                    ) -> {
                error(
                    """
                        PaymentSheet received a SetupIntent in a terminal state.
                        The current SetupIntent has status '${stripeIntent.status}'.
                        See https://stripe.com/docs/api/setup_intents/object#setup_intent_object-status
                    """.trimIndent()
                )
            }
            else -> {
                // valid
            }
        }

        return stripeIntent
    }
}
