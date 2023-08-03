package com.stripe.android.paymentsheet.state

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent

internal sealed class PaymentSheetLoadingException : Throwable() {

    abstract val type: String
    abstract override val cause: Throwable

    data class InvalidConfirmationMethod(
        private val confirmationMethod: PaymentIntent.ConfirmationMethod,
    ) : PaymentSheetLoadingException() {

        override val type: String = "invalidConfirmationMethod"

        override val message: String = """
            PaymentIntent with confirmation_method='automatic' is required.
            The current PaymentIntent has confirmation_method '$confirmationMethod'.
            See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-confirmation_method.
        """.trimIndent()

        override val cause: Throwable
            get() = IllegalStateException(message)
    }

    data class NoPaymentMethodTypesAvailable(
        private val requested: String,
        private val supported: String,
    ) : PaymentSheetLoadingException() {

        override val type: String = "noPaymentMethodTypesAvailable"

        override val message: String
            get() = "None of the requested payment methods ($requested) " +
                "match the supported payment types ($supported)."

        override val cause: Throwable
            get() = IllegalStateException(message)
    }

    data class PaymentIntentInTerminalState(
        private val status: StripeIntent.Status?,
    ) : PaymentSheetLoadingException() {

        override val type: String = "paymentIntentInTerminalState"

        override val message: String
            get() = """
                PaymentSheet cannot set up a PaymentIntent in status '$status'.
                See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-status.
            """.trimIndent()

        override val cause: Throwable
            get() = IllegalStateException(message)
    }

    data class SetupIntentInTerminalState(
        private val status: StripeIntent.Status?,
    ) : PaymentSheetLoadingException() {

        override val type: String = "setupIntentInTerminalState"

        override val message: String
            get() = """
                PaymentSheet cannot set up a SetupIntent in status '$status'.
                See https://stripe.com/docs/api/setup_intents/object#setup_intent_object-status.
            """.trimIndent()

        override val cause: Throwable
            get() = IllegalStateException(message)
    }

    object MissingAmountOrCurrency : PaymentSheetLoadingException() {
        override val type: String = "missingAmountOrCurrency"
        override val message: String = "PaymentIntent must contain amount and currency."

        override val cause: Throwable
            get() = IllegalStateException(message)
    }

    data class Unknown(
        override val cause: Throwable,
    ) : PaymentSheetLoadingException() {
        override val type: String = "unknown"
        override val message: String? = cause.message
    }

    fun unwrap(): Throwable = cause

    companion object {

        fun create(throwable: Throwable): PaymentSheetLoadingException {
            return (throwable as? PaymentSheetLoadingException) ?: Unknown(cause = throwable)
        }
    }
}
