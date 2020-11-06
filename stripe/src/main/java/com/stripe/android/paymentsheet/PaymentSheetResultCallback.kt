package com.stripe.android.paymentsheet

internal fun interface PaymentSheetResultCallback {
    fun onComplete(paymentResult: PaymentResult)
}
