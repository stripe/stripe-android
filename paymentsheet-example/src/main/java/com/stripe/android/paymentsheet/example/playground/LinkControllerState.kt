package com.stripe.android.paymentsheet.example.playground

import com.stripe.android.link.LinkController

internal data class LinkControllerState(
    val selectedPaymentMethodState: LinkController.SelectedPaymentMethodState? = null,
    val lookupConsumerResult: LinkController.LookupConsumerResult? = null,
    val createPaymentMethodResult: LinkController.CreatePaymentMethodResult? = null,
) {
    val paymentMethodPreview: LinkController.PaymentMethodPreview?
        get() = selectedPaymentMethodState?.preview
}
