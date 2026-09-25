package com.stripe.android.paymentsheet.example.playground.checkout.settings

internal object CheckoutShippingAddressDefinitions {
    val shouldSetConfiguration = boolean(
        key = "controller.shipping_address.should_set_configuration",
        displayName = "Set configuration",
    )

    val configuration: CheckoutPlaygroundSettingDefinition.Configuration = configuration(
        key = "controller.shipping_address",
        displayName = "Shipping Address Element",
        children = arrayOf(shouldSetConfiguration),
    )
}
