package com.stripe.android.link

import com.stripe.android.link.gate.LinkGate

internal data class LinkControllerState(
    val linkConfigurationResult: Result<LinkConfiguration?>? = null,
    val linkGate: LinkGate? = null,
    val presentedForEmail: String? = null,
    val selectedPaymentMethod: LinkPaymentMethod? = null,
    val selectedPaymentMethodState: LinkController.SelectedPaymentMethodState =
        LinkController.SelectedPaymentMethodState(),
    val lookupConsumerResult: LinkController.LookupConsumerResult? = null,
    val createPaymentMethodResult: LinkController.CreatePaymentMethodResult? = null,
) {
    val linkConfiguration: LinkConfiguration? = linkConfigurationResult?.getOrNull()
}
