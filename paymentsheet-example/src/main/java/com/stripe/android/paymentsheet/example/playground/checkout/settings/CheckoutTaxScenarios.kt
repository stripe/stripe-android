@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.paymentsheet.example.playground.settings.Merchant

internal object CheckoutTaxScenarios {
    private val session = CheckoutPlaygroundDefinitions.session

    val group = group(
        "tax",
        "Tax",
        leaf("no_tax", "No tax") {
            set(session.automaticTax, false)
        },
        leaf("automatic_tax_automatic_billing", "Automatic tax — automatic billing") {
            set(session.merchant, Merchant.US_TAX)
            set(session.automaticTax, true)
            set(session.billingAddressCollection, false)
        },
        leaf("automatic_tax_required_billing", "Automatic tax — required billing") {
            set(session.merchant, Merchant.US_TAX)
            set(session.automaticTax, true)
            set(session.billingAddressCollection, true)
        },
        leaf("automatic_tax_shipping", "Automatic tax — shipping") {
            set(session.merchant, Merchant.US_TAX)
            set(session.automaticTax, true)
            set(session.shippingAddressCollection, true)
        },
    )
}
