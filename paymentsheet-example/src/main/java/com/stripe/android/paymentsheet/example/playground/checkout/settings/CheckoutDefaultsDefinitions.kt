package com.stripe.android.paymentsheet.example.playground.checkout.settings

internal object CheckoutDefaultsDefinitions {
    val email = optionalText(
        key = "controller.defaults.email",
        displayName = "Email",
        validate = optionalEmail,
    )
    val billing = CheckoutContactDetailsDefinitions(
        key = "controller.defaults.billing",
        displayName = "Billing details",
    )
    val shipping = CheckoutContactDetailsDefinitions(
        key = "controller.defaults.shipping",
        displayName = "Shipping details",
    )
    val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
        key = "controller.defaults",
        displayName = "Defaults",
        children = arrayOf(
            email,
            billing.configuration,
            shipping.configuration,
        ),
    )
}
