package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.FeatureState

internal object CustomerSessionSaveSettingsDefinition : BooleanSettingsDefinition(
    defaultValue = true,
    displayName = "Customer Session Save",
    key = "customer_session_payment_method_save"
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        PlaygroundSettingDefinition.Displayable.Option("Enabled", true),
        PlaygroundSettingDefinition.Displayable.Option("Disabled", false),
    )

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        if (value) {
            checkoutRequestBuilder.paymentMethodSaveFeature(FeatureState.Enabled)
        } else {
            checkoutRequestBuilder.paymentMethodSaveFeature(FeatureState.Disabled)
        }
    }
}
