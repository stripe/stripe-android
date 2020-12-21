package com.stripe.android.paymentsheet

fun interface PaymentSheetResultCallback {
    fun onComplete(paymentResult: PaymentResult)
}
