package com.stripe.android.paymentsheet

import com.stripe.android.paymentelement.FlowControllerPaymentOptionResultPreview
import com.stripe.android.paymentsheet.model.PaymentOption

@FlowControllerPaymentOptionResultPreview
sealed interface PaymentOptionResult {
    val option: PaymentOption?

    class Selected(override val option: PaymentOption?) : PaymentOptionResult

    class Canceled(override val option: PaymentOption?) : PaymentOptionResult
}
