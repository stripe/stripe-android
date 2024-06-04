package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CustomerSessionSettingsDefinition : BooleanSettingsDefinition(
    defaultValue = false,
    displayName = "Use Customer Session",
    key = "customer_session_enabled"
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        if (value) {
            checkoutRequestBuilder.customerKeyType(CheckoutRequest.CustomerKeyType.CustomerSession)
        } else {
            checkoutRequestBuilder.customerKeyType(CheckoutRequest.CustomerKeyType.Legacy)
        }
    }
}
