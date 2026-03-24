@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.checkout.Checkout
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CheckoutSessionAdaptivePricingSettingsDefinition : BooleanSettingsDefinition(
    defaultValue = false,
    displayName = "Adaptive Pricing",
    key = "checkout_session_adaptive_pricing"
) {
    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return settings[InitializationTypeSettingsDefinition] == InitializationType.CheckoutSession
    }

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.adaptivePricing(value)
    }

    override fun configure(value: Boolean, configuration: Checkout.Configuration) {
        configuration.adaptivePricingAllowed(value)
    }
}
