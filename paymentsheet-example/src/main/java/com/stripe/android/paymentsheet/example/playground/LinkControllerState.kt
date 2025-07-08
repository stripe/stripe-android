package com.stripe.android.paymentsheet.example.playground

import com.stripe.android.link.LinkController

internal data class LinkControllerState(
    val presentPaymentMethodsResult: LinkController.PresentPaymentMethodsResult? = null,
    val lookupConsumerResult: LinkController.LookupConsumerResult? = null,
    val createPaymentMethodResult: LinkController.CreatePaymentMethodResult? = null,
) {
    val paymentMethodPreview: LinkController.PaymentMethodPreview?
        get() = (presentPaymentMethodsResult as? LinkController.PresentPaymentMethodsResult.Selected)
            ?.preview
}
