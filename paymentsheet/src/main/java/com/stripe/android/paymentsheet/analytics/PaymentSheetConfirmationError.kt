package com.stripe.android.paymentsheet.analytics

import com.stripe.android.core.exception.StripeException

internal sealed class PaymentSheetConfirmationError : Throwable() {

    abstract val analyticsValue: String

    data class Stripe(override val cause: Throwable) : PaymentSheetConfirmationError() {

        override val analyticsValue: String
            get() = StripeException.create(cause).analyticsValue()
    }

    data class GooglePay(val errorCode: Int) : PaymentSheetConfirmationError() {

        override val analyticsValue: String
            get() = "googlePay_$errorCode"
    }

    object InvalidState : PaymentSheetConfirmationError() {

        override val analyticsValue: String
            get() = "invalidState"
    }
}
