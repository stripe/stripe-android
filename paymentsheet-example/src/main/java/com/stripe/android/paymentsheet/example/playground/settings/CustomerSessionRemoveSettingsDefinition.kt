package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest
import com.stripe.android.paymentsheet.example.playground.model.FeatureState

internal object CustomerSessionRemoveSettingsDefinition : BooleanSettingsDefinition(
    defaultValue = true,
    displayName = "Customer Session Remove",
    key = "customer_session_payment_method_remove"
) {
    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        PlaygroundSettingDefinition.Displayable.Option("Enabled", true),
        PlaygroundSettingDefinition.Displayable.Option("Disabled", false),
    )

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        if (value) {
            checkoutRequestBuilder.paymentMethodRemoveFeature(FeatureState.Enabled)
        } else {
            checkoutRequestBuilder.paymentMethodRemoveFeature(FeatureState.Disabled)
        }
    }

    override fun configure(
        value: Boolean,
        customerEphemeralKeyRequestBuilder: CustomerEphemeralKeyRequest.Builder
    ) {
        if (value) {
            customerEphemeralKeyRequestBuilder.paymentMethodRemoveFeature(FeatureState.Enabled)
        } else {
            customerEphemeralKeyRequestBuilder.paymentMethodRemoveFeature(FeatureState.Disabled)
        }
    }
}
