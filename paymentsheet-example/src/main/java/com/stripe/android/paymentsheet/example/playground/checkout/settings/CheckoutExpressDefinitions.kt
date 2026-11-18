@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.uicore.StripeThemeDefaults

internal object CheckoutExpressDefinitions {
    val shouldSetConfiguration = boolean(
        key = "controller.express_checkout.should_set_configuration",
        displayName = "Set configuration",
    )

    val link = LinkDefinitions()

    internal class LinkDefinitions {
        val display = enumChoice(
            key = "express.link.display",
            displayName = "Display",
            defaultValue = ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Automatic,
        )
        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = "express.link",
            displayName = "Link",
            children = arrayOf(display),
        )
    }

    val googlePay = CheckoutGooglePayDefinitions(
        key = "express.google_pay",
        displayName = "Google Pay",
        displayOptions = ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.entries,
        buttonTypeOptions = ExpressCheckoutElement.Configuration.GooglePayConfiguration.ButtonType.entries,
    )
    val appearance = AppearanceDefinitions()

    internal class AppearanceDefinitions {
        val buttonLayout = ButtonLayoutDefinitions()

        internal class ButtonLayoutDefinitions {
            val maxColumns = optionalInt(
                key = "express.appearance.button_layout.max_columns",
                displayName = "maxColumns",
            )
            val maxRows = optionalInt(
                key = "express.appearance.button_layout.max_rows",
                displayName = "maxRows",
            )
            val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
                key = "express.appearance.button_layout",
                displayName = "buttonLayout",
                children = arrayOf(maxColumns, maxRows),
            )
        }

        val buttonTheme = enumChoice(
            key = "express.appearance.button_theme",
            displayName = "buttonTheme",
            defaultValue = ExpressCheckoutElement.Configuration.Appearance.ButtonTheme.Automatic,
        )
        val cornerRadiusDp = decimal(
            key = "express.appearance.corner_radius_dp",
            displayName = "cornerRadiusDp",
            defaultValue = StripeThemeDefaults.shapes.cornerRadius,
        )

        val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
            key = "express.appearance",
            displayName = "appearance",
            children = arrayOf(buttonLayout.configuration, buttonTheme, cornerRadiusDp),
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
                appearance.configuration,
            ),
        )
    }
}
