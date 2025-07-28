package com.stripe.android.paymentsheet

import com.stripe.android.paymentelement.FlowControllerPaymentOptionResultPreview

@FlowControllerPaymentOptionResultPreview
fun interface PaymentOptionResultCallback {
    /**
     * @param paymentOptionResult The new [PaymentOptionResult]. Indicates the action the user has taken using the
     *   payment option selection sheets.
     */
    fun onPaymentOptionResult(paymentOptionResult: PaymentOptionResult)
}
