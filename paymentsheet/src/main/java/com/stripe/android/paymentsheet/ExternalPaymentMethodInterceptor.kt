package com.stripe.android.paymentsheet

internal object ExternalPaymentMethodHandler {

    fun confirm(onComplete: (PaymentSheetResult) -> Unit) {
        onComplete(PaymentSheetResult.Failed(error = NotImplementedError("Not implemented yet!")))
    }
}
