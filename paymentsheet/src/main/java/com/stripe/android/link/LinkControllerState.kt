package com.stripe.android.link

import com.stripe.android.link.gate.LinkGate

internal data class LinkControllerState(
    val linkConfigurationResult: Result<LinkConfiguration?>? = null,
    val linkGate: LinkGate? = null,
    val presentedForEmail: String? = null,
    val selectedPaymentMethod: LinkPaymentMethod? = null,
    val presentPaymentMethodsResult: LinkController.PresentPaymentMethodsResult? = null,
    val presentForAuthenticationResult: LinkController.PresentForAuthenticationResult? = null,
    val lookupConsumerResult: LinkController.LookupConsumerResult? = null,
    val createPaymentMethodResult: LinkController.CreatePaymentMethodResult? = null,
    val registerNewLinkUserResult: LinkController.RegisterNewLinkUserResult? = null,
) {
    val linkConfiguration: LinkConfiguration? = linkConfigurationResult?.getOrNull()

    val paymentMethodPreview: LinkController.PaymentMethodPreview?
        get() = presentPaymentMethodsResult?.preview
}
