package com.stripe.android.paymentsheet

import com.stripe.android.paymentelement.FlowControllerPaymentOptionResultPreview

@FlowControllerPaymentOptionResultPreview
fun interface PaymentOptionResultCallback {
    /**
     * @param paymentOptionResult The new [PaymentOptionResult]. Contains the updated
     *   payment option and action it was received from.
     */
    fun onPaymentOptionResult(paymentOptionResult: PaymentOptionResult)
}
