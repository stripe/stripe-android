package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest
import com.stripe.android.paymentsheet.example.playground.model.FeatureState

internal object CustomerSessionRemoveSettingsDefinition : BooleanSettingsDefinition(
    defaultValue = true,
    displayName = "Customer Session Remove",
    key = "customer_session_payment_method_remove"
) {
    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        if (!configurationData.integrationType.isPaymentFlow() && !configurationData.integrationType.isCustomerFlow()) {
            return false
        }

        return settings[CustomerSessionSettingsDefinition] == true
    }

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.paymentMethodRemoveFeature(FeatureState.fromBoolean(value))
    }

    override fun configure(
        value: Boolean,
        customerEphemeralKeyRequestBuilder: CustomerEphemeralKeyRequest.Builder
    ) {
        customerEphemeralKeyRequestBuilder.paymentMethodRemoveFeature(FeatureState.fromBoolean(value))
    }
}
