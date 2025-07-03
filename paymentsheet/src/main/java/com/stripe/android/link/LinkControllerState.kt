package com.stripe.android.link

import com.stripe.android.link.gate.LinkGate

internal data class LinkControllerState(
    val linkConfigurationResult: Result<LinkConfiguration?>? = null,
    val linkGate: LinkGate? = null,
    val presentedForEmail: String? = null,
    val linkAccountUpdate: LinkAccountUpdate = LinkAccountUpdate.None,
    val selectedPaymentMethod: LinkPaymentMethod? = null,
    val presentPaymentMethodsResult: LinkController.PresentPaymentMethodsResult? = null,
    val lookupConsumerResult: LinkController.LookupConsumerResult? = null,
) {
    val paymentMethodPreview: LinkController.PaymentMethodPreview?
        get() = (presentPaymentMethodsResult as? LinkController.PresentPaymentMethodsResult.Selected)
            ?.preview

    val linkConfiguration: LinkConfiguration? = linkConfigurationResult?.getOrNull()
}
