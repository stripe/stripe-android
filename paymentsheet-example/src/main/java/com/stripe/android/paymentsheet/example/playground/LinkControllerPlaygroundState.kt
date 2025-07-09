package com.stripe.android.paymentsheet.example.playground

import com.stripe.android.link.LinkController

internal data class LinkControllerPlaygroundState(
    val state: LinkController.State? = null,
    val presentPaymentMethodsResult: LinkController.PresentPaymentMethodsResult? = null,
    val lookupConsumerResult: LinkController.LookupConsumerResult? = null,
    val createPaymentMethodResult: LinkController.CreatePaymentMethodResult? = null,
)
