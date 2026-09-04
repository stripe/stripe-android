@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

internal object CheckoutPlaygroundDefinitions {
    val root: CheckoutPlaygroundSettingDefinition.Configuration by lazy {
        configuration(
            key = "root",
            displayName = "Checkout Controller Playground",
            children = arrayOf(
                session.configuration,
                Controller.configuration,
            ),
        )
    }

    val session = CheckoutSessionDefinitions

    object Controller {
        val merchantDisplayName = optionalText(
            key = "controller.merchant_display_name",
            displayName = "Merchant display name",
        )
        val defaults = CheckoutDefaultsDefinitions
        val payment = CheckoutPaymentDefinitions
        val currencySelector = CheckoutCurrencySelectorDefinitions
        val express = CheckoutExpressDefinitions

        val configuration: CheckoutPlaygroundSettingDefinition.Configuration by lazy {
            configuration(
                key = "controller",
                displayName = "CheckoutController.Configuration",
                children = arrayOf(
                    merchantDisplayName,
                    defaults.configuration,
                    payment.configuration,
                    currencySelector.configuration,
                    express.configuration,
                ),
            )
        }
    }
}
