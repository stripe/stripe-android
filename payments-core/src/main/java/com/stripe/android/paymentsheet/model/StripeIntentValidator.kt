package com.stripe.android.paymentsheet.model

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent

/**
 * Validator for [PaymentIntent] or [SetupIntent] instances used in PaymentSheet.
 */
internal class StripeIntentValidator {
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
                        The current PaymentIntent has confirmation_method ${stripeIntent.confirmationMethod}.
                        See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-confirmation_method.
                    """.trimIndent()
                )
            }
            stripeIntent is PaymentIntent &&
                (
                    (stripeIntent.status != StripeIntent.Status.RequiresPaymentMethod) &&
                        (stripeIntent.status != StripeIntent.Status.RequiresAction)
                    ) -> {
                error(
                    """
                        A PaymentIntent with status='requires_payment_method' or 'requires_action` is required.
                        The current PaymentIntent has status ${stripeIntent.status}.
                        See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-status.
                    """.trimIndent()
                )
            }
            stripeIntent is SetupIntent &&
                (
                    (stripeIntent.status != StripeIntent.Status.RequiresPaymentMethod) &&
                        (stripeIntent.status != StripeIntent.Status.RequiresAction)
                    ) -> {
                error(
                    """
                        A SetupIntent with status='requires_payment_method' or 'requires_action` is required.
                        The current SetupIntent has status ${stripeIntent.status}.
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
