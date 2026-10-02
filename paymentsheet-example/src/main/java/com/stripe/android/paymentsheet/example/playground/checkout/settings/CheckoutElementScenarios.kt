@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.PaymentElement
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.LinkType
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
            eceLeaf("default", "Default") {},
            leaf("hidden", "No configuration (hidden)") {
                showWalletsInExpressOnly()
            },
            eceLeaf("google_pay_only", "Google Pay only") {
                set(
                    controller.express.link.display,
                    ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never,
                )
            },
            eceLeaf("link_native", "Link only — native") {
                set(
                    controller.express.googlePay.display,
                    ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Never,
                )
                set(session.linkType, LinkType.Native)
            },
            eceLeaf("recognized_link", "Link for existing users") {
                set(
                    controller.express.link.display,
                    ExpressCheckoutElement.Configuration.LinkConfiguration.Display.WalletButtonHidden,
                )
                set(session.linkType, LinkType.NativeAttest)
            },
            eceLeaf("restricted_link", "Link restrictions + billing") {
                set(
                    controller.express.googlePay.display,
                    ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Never,
                )
                set(controller.express.link.collectMissingBilling, false)
                set(controller.express.link.disallowedFunding, listOf("card", "bank_account"))
                set(session.billingAddressCollection, true)
                set(session.linkType, LinkType.Web)
            },
            eceLeaf("custom_google_pay", "Custom Google Pay") {
                set(
                    controller.express.googlePay.buttonType,
                    ExpressCheckoutElement.Configuration.GooglePayConfiguration.ButtonType.Checkout,
                )
                set(controller.express.googlePay.label, "ECE total")
                set(controller.express.googlePay.additionalNetworks, listOf("INTERAC"))
                set(controller.merchantDisplayName, "ECE Store")
            },
            eceLeaf("google_pay_first_row", "Dark row — Google Pay first") {
                set(
                    controller.express.appearance.theme,
                    ExpressCheckoutElement.Configuration.Appearance.ButtonTheme.Dark,
                )
                set(controller.express.appearance.layout.columns, 2)
                set(controller.express.appearance.layout.rows, 1)
                set(controller.express.paymentMethodOrder, listOf("google_pay", "link"))
            },
            eceLeaf("link_first_slot", "Light slot — Link first") {
                set(
                    controller.express.appearance.theme,
                    ExpressCheckoutElement.Configuration.Appearance.ButtonTheme.Light,
                )
                set(controller.express.appearance.layout.columns, 1)
                set(controller.express.appearance.layout.rows, 1)
                set(controller.express.paymentMethodOrder, listOf("link", "google_pay"))
            },
            eceLeaf("shipping_required", "Shipping required") {
                set(session.shippingAddressCollection, true)
            },
            eceLeaf("auto_tax_shipping", "Auto tax + shipping") {
                set(session.automaticTax, true)
                set(session.shippingAddressCollection, true)
            },
            eceLeaf("billing_required", "Billing required") {
                set(session.billingAddressCollection, true)
            },
            eceLeaf("auto_tax_billing", "Auto tax + billing") {
                set(session.automaticTax, true)
                set(session.billingAddressCollection, true)
            },
            eceLeaf("missing_email", "Missing email") {
                set(session.customerEmail, "")
            },
            eceLeaf("prefilled_defaults", "Required addresses + defaults") {
                set(session.customerEmail, "")
                set(session.billingAddressCollection, true)
                set(session.shippingAddressCollection, true)
                set(controller.defaults.email, "jenny.rosen@example.com")
                setContactDefaults(controller.defaults.billing)
                setContactDefaults(controller.defaults.shipping)
            },
        ),
        group(
            "sae",
            "SAE",
            leaf("collect_new_shipping", "Collect a new shipping address") {
                set(controller.shippingAddress.shouldSetConfiguration, true)
                set(session.shippingAddressCollection, true)
            },
            leaf("prefilled_us_shipping", "Prefilled US shipping address") {
                set(controller.shippingAddress.shouldSetConfiguration, true)
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
            set(controller.shippingAddress.shouldSetConfiguration, true)
            set(session.shippingAddressCollection, true)
            set(session.adaptivePricingCountry, AdaptivePricingCountry.France)
        },
    )

    private fun eceLeaf(
        key: String,
        name: String,
        block: CheckoutPlaygroundPresetBuilder.() -> Unit,
    ) = leaf(key, name) {
        set(controller.express.shouldSetConfiguration, true)
        showWalletsInExpressOnly()
        block()
    }

    private fun CheckoutPlaygroundPresetBuilder.setContactDefaults(
        definitions: CheckoutContactDetailsDefinitions,
    ) {
        set(definitions.enabled, true)
        set(definitions.name, "Jenny Rosen")
        set(definitions.address.enabled, true)
        set(definitions.address.country, "US")
        set(definitions.address.city, "San Francisco")
        set(definitions.address.line1, "510 Townsend St")
        set(definitions.address.postalCode, "94103")
        set(definitions.address.state, "CA")
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
