package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object ConfirmationTokenSettingsDefinition : BooleanSettingsDefinition(
    key = "confirmationToken",
    displayName = "Use Confirmation Token",
    defaultValue = true,
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.isConfirmationToken(value)
    }

    override fun valueUpdated(value: Boolean, playgroundSettings: PlaygroundSettings) {
        if (value) {
            playgroundSettings[CustomerSessionSettingsDefinition] = true
        }
    }
}
