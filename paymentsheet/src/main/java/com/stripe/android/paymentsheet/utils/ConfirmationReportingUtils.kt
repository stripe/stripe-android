package com.stripe.android.paymentsheet.utils

import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationTypeKey
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun ConfirmationHandler.Result.Failed.toConfirmationError(): PaymentSheetConfirmationError? {
    return if (type is ConfirmationHandler.Result.Failed.ErrorType.Transaction) {
        PaymentSheetConfirmationError(
            cause = cause,
            analyticsValue = type.analyticsValue,
            errorCode = type.errorCode,
        )
    } else {
        null
    }
}

internal fun EventReporter.reportPaymentResult(
    result: ConfirmationHandler.Result,
    paymentSelection: PaymentSelection?
) {
    paymentSelection?.let { selection ->
        when (result) {
            is ConfirmationHandler.Result.Succeeded -> onPaymentSuccess(
                paymentSelection = selection,
                deferredIntentConfirmationType = result.metadata[DeferredIntentConfirmationTypeKey],
                intentId = result.intent.id,
            )
            is ConfirmationHandler.Result.Failed -> {
                result.toConfirmationError()?.let { confirmationError ->
                    onPaymentFailure(selection, confirmationError)
                }
            }
            is ConfirmationHandler.Result.Canceled -> {}
        }
    }
}
