package com.stripe.android.paymentsheet

import com.stripe.android.paymentelement.FlowControllerPaymentOptionResultPreview
import com.stripe.android.paymentsheet.model.PaymentOption

@FlowControllerPaymentOptionResultPreview
sealed interface PaymentOptionResult {
    val paymentOption: PaymentOption?

    class Selected(
        override val paymentOption: PaymentOption
    ) : PaymentOptionResult

    class Canceled(
        override val paymentOption: PaymentOption?
    ) : PaymentOptionResult
}
