package com.stripe.android.paymentsheet.analytics

import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.exception.StripeException

internal sealed class PaymentSheetConfirmationError : Throwable() {

    abstract val analyticsValue: String

    data class Stripe(override val cause: Throwable) : PaymentSheetConfirmationError() {

        override val analyticsValue: String
            get() = StripeException.create(cause).analyticsValue
    }

    data class GooglePay(val errorCode: Int) : PaymentSheetConfirmationError() {

        override val analyticsValue: String
            get() = "googlePay_$errorCode"
    }

    @Suppress("DataClassPrivateConstructor")
    data class InvalidState private constructor(
        override val analyticsValue: String,
    ) : PaymentSheetConfirmationError() {

        companion object {
            fun inProgressConfiguration(): InvalidState {
                return InvalidState("inProgressConfiguration")
            }

            fun missingState(): InvalidState {
                return InvalidState("invalidState")
            }
        }
    }
}

internal val StripeException.analyticsValue: String
    get() = when (this) {
        is APIException -> "apiError"
        is APIConnectionException -> "connectionError"
        is InvalidRequestException -> "invalidRequestError"
        else -> "unknown"
    }
