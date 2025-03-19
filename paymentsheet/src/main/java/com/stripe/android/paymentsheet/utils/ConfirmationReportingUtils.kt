package com.stripe.android.paymentsheet.utils

import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun ConfirmationHandler.Result.Failed.toConfirmationError(): PaymentSheetConfirmationError? {
    return when (type) {
        ConfirmationHandler.Result.Failed.ErrorType.ExternalPaymentMethod ->
            PaymentSheetConfirmationError.ExternalPaymentMethod
        ConfirmationHandler.Result.Failed.ErrorType.Payment ->
            PaymentSheetConfirmationError.Stripe(cause)
        is ConfirmationHandler.Result.Failed.ErrorType.GooglePay ->
            PaymentSheetConfirmationError.GooglePay(type.errorCode)
        ConfirmationHandler.Result.Failed.ErrorType.Internal,
        ConfirmationHandler.Result.Failed.ErrorType.MerchantIntegration,
        ConfirmationHandler.Result.Failed.ErrorType.Fatal -> null
    }
}

internal fun EventReporter.reportPaymentResult(
    result: ConfirmationHandler.Result,
    paymentSelection: PaymentSelection?
) {
    when (result) {
        is ConfirmationHandler.Result.Succeeded -> onPaymentSuccess(
            paymentSelection,
            result.deferredIntentConfirmationType
        )
        is ConfirmationHandler.Result.Failed -> {
            result.toConfirmationError()?.let { confirmationError ->
                onPaymentFailure(paymentSelection, confirmationError)
            }
        }
        is ConfirmationHandler.Result.Canceled -> {}
    }
}
