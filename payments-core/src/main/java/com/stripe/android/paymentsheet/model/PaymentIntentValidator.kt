package com.stripe.android.paymentsheet.model

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent

/**
 * Validator for [PaymentIntent] instances used in PaymentSheet.
 */
internal class PaymentIntentValidator {
    @JvmSynthetic
    fun requireValid(
        paymentIntent: PaymentIntent
    ): PaymentIntent {
        when {
            paymentIntent.confirmationMethod != PaymentIntent.ConfirmationMethod.Automatic -> {
                error(
                    """
                        PaymentIntent with confirmation_method='automatic' is required.
                        See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-confirmation_method.
                    """.trimIndent()
                )
            }
            paymentIntent.status != StripeIntent.Status.RequiresPaymentMethod -> {
                error(
                    """
                        A PaymentIntent with status='requires_payment_method' is required.
                        The current PaymentIntent has status ${paymentIntent.status}.
                        See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-status.
                    """.trimIndent()
                )
            }
            else -> {
                // valid
            }
        }

        return paymentIntent
    }
}
