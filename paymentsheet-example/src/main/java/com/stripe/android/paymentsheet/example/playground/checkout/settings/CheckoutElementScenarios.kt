@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.PaymentElement
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.Merchant

internal object CheckoutElementScenarios {
    private val session = CheckoutPlaygroundDefinitions.session
    private val controller = CheckoutPlaygroundDefinitions.Controller

    val group = group(
        "elements",
        "Elements",
        leaf("payment_element", "Payment Element — card only") {
            regional(Merchant.US, Currency.USD, PaymentMethod.Type.Card)
            set(controller.payment.shouldSetConfiguration, true)
        },
        group(
            "ece",
            "ECE",
            eceLeaf("link_google_pay", "Link and Google Pay"),
            eceLeaf("link_only", "Link only", googlePay = false),
            eceLeaf("google_pay_only", "Google Pay only", link = false),
            eceLeaf("shipping_required", "Shipping required", shipping = true),
        ),
        group(
            "sae",
            "SAE",
            leaf("collect_new_shipping", "Collect a new shipping address") {
                set(session.shippingAddressCollection, true)
            },
            leaf("prefilled_us_shipping", "Prefilled US shipping address") {
                set(controller.defaults.shipping.enabled, true)
                set(controller.defaults.shipping.name, "Jenny Rosen")
                set(controller.defaults.shipping.address.enabled, true)
                set(controller.defaults.shipping.address.country, "US")
                set(controller.defaults.shipping.address.city, "San Francisco")
                set(controller.defaults.shipping.address.line1, "510 Townsend St")
                set(controller.defaults.shipping.address.postalCode, "94103")
                set(controller.defaults.shipping.address.state, "CA")
            },
        ),
        group(
            "currency_selector",
            "Currency Selector",
            currencySelectorLeaf("france", "France", AdaptivePricingCountry.France),
            currencySelectorLeaf("japan", "Japan", AdaptivePricingCountry.Japan),
        ),
        leaf("all_elements", "All elements") {
            set(controller.payment.shouldSetConfiguration, true)
            set(controller.express.shouldSetConfiguration, true)
            showWalletsInExpressOnly()
            set(controller.currencySelector.shouldSetConfiguration, true)
            set(session.shippingAddressCollection, true)
            set(session.adaptivePricingCountry, AdaptivePricingCountry.France)
        },
    )

    private fun eceLeaf(
        key: String,
        name: String,
        link: Boolean = true,
        googlePay: Boolean = true,
        shipping: Boolean = false,
    ) = leaf(key, name) {
        set(controller.express.shouldSetConfiguration, true)
        showWalletsInExpressOnly()
        set(
            controller.express.link.display,
            if (link) {
                ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Automatic
            } else {
                ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never
            },
        )
        set(
            controller.express.googlePay.display,
            if (googlePay) {
                ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Automatic
            } else {
                ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Never
            },
        )
        if (shipping) {
            set(session.shippingAddressCollection, true)
        }
    }

    private fun CheckoutPlaygroundPresetBuilder.showWalletsInExpressOnly() {
        set(
            controller.payment.link.display,
            PaymentElement.Configuration.LinkConfiguration.Display.Never,
        )
        set(
            controller.payment.googlePay.display,
            PaymentElement.Configuration.GooglePayConfiguration.Display.Never,
        )
    }

    private fun currencySelectorLeaf(
        key: String,
        name: String,
        country: AdaptivePricingCountry,
    ) = leaf(key, name) {
        set(controller.currencySelector.shouldSetConfiguration, true)
        set(session.adaptivePricingCountry, country)
    }
}
