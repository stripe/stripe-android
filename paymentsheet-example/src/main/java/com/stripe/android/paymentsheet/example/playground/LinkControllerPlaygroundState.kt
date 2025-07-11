package com.stripe.android.paymentsheet.example.playground

import com.stripe.android.link.LinkController

data class LinkControllerPlaygroundState(
    val presentPaymentMethodsResult: LinkController.PresentPaymentMethodsResult? = null,
    val lookupConsumerResult: LinkController.LookupConsumerResult? = null,
    val createPaymentMethodResult: LinkController.CreatePaymentMethodResult? = null,
    val presentForAuthenticationResult: LinkController.PresentForAuthenticationResult? = null,
)
