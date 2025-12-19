package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object ConfirmationTokenSettingsDefinition : BooleanSettingsDefinition(
    key = "confirmationToken",
    displayName = "Use Confirmation Token",
    defaultValue = false,
) {
    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        if (!configurationData.integrationType.isPaymentFlow()) {
            return false
        }

        return when (settings[InitializationTypeSettingsDefinition] as InitializationType) {
            InitializationType.Normal -> false
            InitializationType.DeferredClientSideConfirmation,
            InitializationType.DeferredServerSideConfirmation,
            InitializationType.DeferredManualConfirmation,
            InitializationType.DeferredMultiprocessor -> true
        }
    }

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.isConfirmationToken(value)
    }
}
