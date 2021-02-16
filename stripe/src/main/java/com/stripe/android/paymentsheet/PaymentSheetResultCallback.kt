package com.stripe.android.paymentsheet

/**
 * Callback that is invoked when a [PaymentResult] is available.
 */
fun interface PaymentSheetResultCallback {
    fun onPaymentResult(paymentResult: PaymentResult)
}
