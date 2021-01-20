package com.stripe.android.paymentsheet

internal fun interface PaymentSheetResultCallback {
    fun onPaymentResult(paymentResult: PaymentResult)
}
