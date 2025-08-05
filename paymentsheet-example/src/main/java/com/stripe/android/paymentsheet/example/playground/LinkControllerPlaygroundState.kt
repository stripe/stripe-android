package com.stripe.android.paymentsheet.example.playground

import com.stripe.android.link.LinkController

data class LinkControllerPlaygroundState(
    val controllerState: LinkController.State? = null,
    val configureResult: LinkController.ConfigureResult? = null,
    val presentPaymentMethodsResult: LinkController.PresentPaymentMethodsResult? = null,
    val lookupConsumerResult: LinkController.LookupConsumerResult? = null,
    val createPaymentMethodResult: LinkController.CreatePaymentMethodResult? = null,
    val authenticationResult: LinkController.AuthenticationResult? = null,
    val registerConsumerResult: LinkController.RegisterConsumerResult? = null,
)
