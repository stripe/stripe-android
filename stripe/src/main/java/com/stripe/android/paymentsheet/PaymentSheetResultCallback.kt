package com.stripe.android.paymentsheet

fun interface PaymentSheetResultCallback {
    fun onPaymentResult(paymentResult: PaymentResult)
}
