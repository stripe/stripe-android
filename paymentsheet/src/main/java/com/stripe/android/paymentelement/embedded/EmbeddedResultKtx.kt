package com.stripe.android.paymentelement.embedded

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler

@ExperimentalEmbeddedPaymentElementApi
internal fun ConfirmationHandler.Result.asEmbeddedResult(): EmbeddedPaymentElement.Result {
    return when (this) {
        is ConfirmationHandler.Result.Canceled -> {
            EmbeddedPaymentElement.Result.Canceled()
        }
        is ConfirmationHandler.Result.Failed -> {
            EmbeddedPaymentElement.Result.Failed(cause)
        }
        is ConfirmationHandler.Result.Succeeded -> {
            EmbeddedPaymentElement.Result.Completed()
        }
    }
}
