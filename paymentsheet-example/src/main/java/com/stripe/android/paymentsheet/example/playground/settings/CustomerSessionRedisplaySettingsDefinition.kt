package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.FeatureState

internal object CustomerSessionRedisplaySettingsDefinition : BooleanSettingsDefinition(
    defaultValue = true,
    displayName = "Customer Session Redisplay",
    key = "customer_session_payment_method_redisplay"
) {
    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        if (!configurationData.integrationType.isPaymentFlow()) {
            return false
        }

        return settings[CustomerSessionSettingsDefinition] == true
    }

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.paymentMethodRedisplayFeature(FeatureState.fromBoolean(value))
    }
}
