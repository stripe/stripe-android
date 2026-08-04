@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler

internal fun ConfirmationHandler.Result.toCheckoutControllerResult(): CheckoutController.Result {
    return when (this) {
        is ConfirmationHandler.Result.Succeeded -> CheckoutController.Result.Completed()
        is ConfirmationHandler.Result.Failed -> CheckoutController.Result.Failed(cause)
        is ConfirmationHandler.Result.Canceled -> CheckoutController.Result.Canceled()
    }
}
