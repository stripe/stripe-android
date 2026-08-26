@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.elements.ExpressCheckoutElement

internal object CheckoutExpressDefinitions {
    val shouldSetConfiguration = boolean(
        key = "controller.express_checkout.should_set_configuration",
        displayName = "Set configuration",
        defaultValue = false,
    )

    val link = LinkDefinitions()

    internal class LinkDefinitions {
        val display = enumChoice(
            key = "express.link.display",
            displayName = "Display",
            defaultValue = ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Automatic,
        )
        val collectMissingBilling = boolean(
            key = "express.link.collect_missing_billing",
            displayName = "Collect missing billing details",
            defaultValue = true,
        )
        val disallowedFunding = stringCsv(
            key = "express.link.disallow_funding",
            displayName = "Disallowed funding sources (comma separated)",
        )
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = "express.link",
            displayName = "Link",
            children = arrayOf(display, collectMissingBilling, disallowedFunding),
        )
    }

    val googlePay = CheckoutGooglePayDefinitions(
        key = "express.google_pay",
        displayName = "Google Pay",
        defaultDisplay = ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Automatic,
        displayOptions = ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.entries,
        defaultButtonType = ExpressCheckoutElement.Configuration.GooglePayConfiguration.ButtonType.Pay,
        buttonTypeOptions = ExpressCheckoutElement.Configuration.GooglePayConfiguration.ButtonType.entries,
    )
    val shippingRequired = boolean(
        key = "express.shipping_required",
        displayName = "Shipping address required",
        defaultValue = false,
    )

    val billing = BillingDefinitions()

    internal class BillingDefinitions {
        val name = enumChoice(
            key = "express.billing.name",
            displayName = "Name",
            defaultValue = ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration
                .CollectionMode.Automatic,
        )
        val email = enumChoice(
            key = "express.billing.email",
            displayName = "Email",
            defaultValue = ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration
                .CollectionMode.Automatic,
        )
        val address = enumChoice(
            key = "express.billing.address",
            displayName = "Address",
            defaultValue = ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration
                .AddressCollectionMode.Automatic,
        )
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = "express.billing",
            displayName = "Billing details collection",
            children = arrayOf(name, email, address),
        )
    }

    val appearance = AppearanceDefinitions()

    internal class AppearanceDefinitions {
        val theme = enumChoice(
            key = "express.appearance.theme",
            displayName = "Button theme",
            defaultValue = ExpressCheckoutElement.Configuration.Appearance.ButtonTheme.Automatic,
        )

        val layout = LayoutDefinitions()

        internal class LayoutDefinitions {
            val columns = optionalInt(
                key = "express.appearance.layout.columns",
                displayName = "Maximum columns",
            )
            val rows = optionalInt(
                key = "express.appearance.layout.rows",
                displayName = "Maximum rows",
            )
            val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
                key = "express.appearance.layout",
                displayName = "Button layout",
                children = arrayOf(columns, rows),
            )
        }

        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = "express.appearance",
            displayName = "Appearance",
            children = arrayOf(theme, layout.configuration),
        )
    }

    val configuration: CheckoutPlaygroundSettingDefinition.Configuration by lazy {
        configuration(
            key = "controller.express_checkout",
            displayName = "Express Checkout Element",
            children = arrayOf(
                shouldSetConfiguration,
                link.configuration,
                googlePay.configuration,
                shippingRequired,
                billing.configuration,
                appearance.configuration,
            ),
        )
    }
}
