package com.stripe.android.paymentsheet

/**
 * Callback that is invoked when a [PaymentSheetResult] is available.
 */
fun interface PaymentSheetResultCallback {
    fun onPaymentResult(paymentSheetResult: PaymentSheetResult)
}
