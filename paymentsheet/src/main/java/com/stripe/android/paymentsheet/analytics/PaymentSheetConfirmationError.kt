package com.stripe.android.paymentsheet.analytics

import com.stripe.android.core.exception.StripeException

internal sealed class PaymentSheetConfirmationError : Throwable() {

    abstract val analyticsValue: String
    abstract val errorCode: String?

    data class Stripe(override val cause: Throwable) : PaymentSheetConfirmationError() {
        private val stripeException = StripeException.create(cause)

        override val errorCode: String? = stripeException.stripeError?.code

        override val analyticsValue: String
            get() = stripeException.analyticsValue()
    }

    data class GooglePay(val errorCodeInt: Int) : PaymentSheetConfirmationError() {
        override val errorCode = errorCodeInt.toString()

        override val analyticsValue: String
            get() = "googlePay_$errorCode"
    }

    data object ExternalPaymentMethod : PaymentSheetConfirmationError() {
        override val errorCode: String? = null

        override val analyticsValue: String
            get() = "externalPaymentMethodError"
    }

    object InvalidState : PaymentSheetConfirmationError() {
        override val errorCode: String? = null

        override val analyticsValue: String
            get() = "invalidState"
    }
}
