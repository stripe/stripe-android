package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object RequireCvcRecollectionDefinition : BooleanSettingsDefinition(
    key = "requireCvcRecollection",
    displayName = "Require CVC Recollection",
    defaultValue = false
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.requireCvcRecollection(value)
    }
}
