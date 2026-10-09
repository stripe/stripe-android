@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.elements.PaymentElement
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.Merchant

internal object CheckoutLinkScenarios {
    val group = group(
        "link_type",
        "Link Type",
        linkLeaf("link_server_controlled", "Server Controlled", native = null, attestation = null),
        linkLeaf("link_native", "Native", native = true, attestation = false),
        linkLeaf("link_native_attest", "Native + Attest", native = true, attestation = true),
        linkLeaf("link_web", "Web", native = false, attestation = false),
    )

    private fun linkLeaf(
        key: String,
        name: String,
        native: Boolean?,
        attestation: Boolean?,
    ) = leaf(key, name) {
        regional(Merchant.US, Currency.USD, PaymentMethod.Type.Card, PaymentMethod.Type.Link)
        val payment = CheckoutPlaygroundDefinitions.Controller.payment
        set(payment.shouldSetConfiguration, true)
        set(payment.link.display, PaymentElement.Configuration.LinkConfiguration.Display.Automatic)
        set(CheckoutFeatureFlagDefinitions.nativeLinkEnabled, native)
        set(CheckoutFeatureFlagDefinitions.nativeLinkAttestationEnabled, attestation)
    }
}
