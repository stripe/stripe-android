package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object DefaultShippingAddressSettingsDefinition : BooleanSettingsDefinition(
    key = "defaultShippingAddress",
    displayName = "Add Default Shipping Address",
    defaultValue = true,
) {
    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.setShippingAddress(value)
    }
}
