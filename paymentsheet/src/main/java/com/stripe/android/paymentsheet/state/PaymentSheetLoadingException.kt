package com.stripe.android.paymentsheet.state

import com.stripe.android.core.exception.StripeException
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.analytics.analyticsValue
import com.stripe.android.paymentsheet.state.PaymentSheetLoadingException.Unknown

internal sealed class PaymentSheetLoadingException : Throwable() {

    abstract val type: String
    abstract val stripeIntent: StripeIntent?

    data class InvalidConfirmationMethod(
        override val stripeIntent: PaymentIntent,
    ) : PaymentSheetLoadingException() {

        private val confirmationMethod: PaymentIntent.ConfirmationMethod
            get() = stripeIntent.confirmationMethod

        override val type: String = "invalidConfirmationMethod"

        override val message: String = """
            PaymentIntent with confirmation_method='automatic' is required.
            The current PaymentIntent has confirmation_method '$confirmationMethod'.
            See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-confirmation_method.
        """.trimIndent()
    }

    data class NoPaymentMethodTypesAvailable(
        override val stripeIntent: StripeIntent,
        private val supported: String,
    ) : PaymentSheetLoadingException() {

        private val requested: String
            get() = stripeIntent.paymentMethodTypes.joinToString(separator = ", ")

        override val type: String = "noPaymentMethodTypesAvailable"

        override val message: String
            get() = "None of the requested payment methods ($requested) " +
                "match the supported payment types ($supported)."
    }

    data class PaymentIntentInTerminalState(
        override val stripeIntent: StripeIntent,
    ) : PaymentSheetLoadingException() {

        private val status: StripeIntent.Status?
            get() = stripeIntent.status

        override val type: String = "paymentIntentInTerminalState"

        override val message: String
            get() = """
                PaymentSheet cannot set up a PaymentIntent in status '$status'.
                See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-status.
            """.trimIndent()
    }

    data class SetupIntentInTerminalState(
        override val stripeIntent: StripeIntent,
    ) : PaymentSheetLoadingException() {

        private val status: StripeIntent.Status?
            get() = stripeIntent.status

        override val type: String = "setupIntentInTerminalState"

        override val message: String
            get() = """
                PaymentSheet cannot set up a SetupIntent in status '$status'.
                See https://stripe.com/docs/api/setup_intents/object#setup_intent_object-status.
            """.trimIndent()
    }

    data class MissingAmountOrCurrency(
        override val stripeIntent: StripeIntent,
    ) : PaymentSheetLoadingException() {
        override val type: String = "missingAmountOrCurrency"
        override val message: String = "PaymentIntent must contain amount and currency."
    }

    data class Unknown(
        override val cause: Throwable,
    ) : PaymentSheetLoadingException() {

        override val type: String
            get() = StripeException.create(cause).analyticsValue

        override val message: String? = cause.message

        override val stripeIntent: StripeIntent? = null
    }
}

internal val Throwable.asPaymentSheetLoadingException: PaymentSheetLoadingException
    get() = (this as? PaymentSheetLoadingException) ?: Unknown(cause = this)
