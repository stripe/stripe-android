package com.stripe.android.paymentsheet.state

import com.stripe.android.core.exception.StripeException
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.state.PaymentSheetLoadingException.Unknown

internal sealed class PaymentSheetLoadingException : Throwable() {

    abstract val type: String
    abstract val usedPaymentMethod: PaymentMethod?

    data class InvalidConfirmationMethod(
        private val confirmationMethod: PaymentIntent.ConfirmationMethod,
    ) : PaymentSheetLoadingException() {

        override val usedPaymentMethod: PaymentMethod? = null

        override val type: String = "invalidConfirmationMethod"

        override val message: String = """
            PaymentIntent with confirmation_method='automatic' is required.
            The current PaymentIntent has confirmation_method '$confirmationMethod'.
            See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-confirmation_method.
        """.trimIndent()
    }

    data class NoPaymentMethodTypesAvailable(
        private val requested: String,
        private val supported: String,
    ) : PaymentSheetLoadingException() {

        override val usedPaymentMethod: PaymentMethod? = null

        override val type: String = "noPaymentMethodTypesAvailable"

        override val message: String
            get() = "None of the requested payment methods ($requested) " +
                "match the supported payment types ($supported)."
    }

    data class PaymentIntentInTerminalState(
        override val usedPaymentMethod: PaymentMethod?,
        private val status: StripeIntent.Status?,
    ) : PaymentSheetLoadingException() {

        override val type: String = "paymentIntentInTerminalState"

        override val message: String
            get() = """
                PaymentSheet cannot set up a PaymentIntent in status '$status'.
                See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-status.
            """.trimIndent()
    }

    data class SetupIntentInTerminalState(
        override val usedPaymentMethod: PaymentMethod?,
        private val status: StripeIntent.Status?,
    ) : PaymentSheetLoadingException() {

        override val type: String = "setupIntentInTerminalState"

        override val message: String
            get() = """
                PaymentSheet cannot set up a SetupIntent in status '$status'.
                See https://stripe.com/docs/api/setup_intents/object#setup_intent_object-status.
            """.trimIndent()
    }

    object MissingAmountOrCurrency : PaymentSheetLoadingException() {
        override val usedPaymentMethod: PaymentMethod? = null
        override val type: String = "missingAmountOrCurrency"
        override val message: String = "PaymentIntent must contain amount and currency."
    }

    data class Unknown(
        override val cause: Throwable,
    ) : PaymentSheetLoadingException() {

        override val type: String
            get() = StripeException.create(cause).analyticsValue()

        override val message: String? = cause.message

        override val usedPaymentMethod: PaymentMethod? = null
    }
}

internal val Throwable.asPaymentSheetLoadingException: PaymentSheetLoadingException
    get() = (this as? PaymentSheetLoadingException) ?: Unknown(cause = this)
