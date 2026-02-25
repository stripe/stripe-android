package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object ConfirmationTokenSettingsDefinition : BooleanSettingsDefinition(
    key = "confirmationToken",
    displayName = "Use Confirmation Token",
    defaultValue = true,
) {
    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        if (!configurationData.integrationType.isPaymentFlow()) {
            return false
        }

        return initializationTypeIsDeferred(settings) && usingCustomerSessionsForSavedPms(settings)
    }

    private fun initializationTypeIsDeferred(settings: Map<PlaygroundSettingDefinition<*>, Any?>): Boolean {
        return when (settings[InitializationTypeSettingsDefinition] as InitializationType) {
            InitializationType.Normal -> false
            InitializationType.DeferredClientSideConfirmation,
            InitializationType.DeferredServerSideConfirmation,
            InitializationType.DeferredManualConfirmation,
            InitializationType.DeferredMultiprocessor,
            InitializationType.CheckoutSession -> true
        }
    }

    private fun usingCustomerSessionsForSavedPms(settings: Map<PlaygroundSettingDefinition<*>, Any?>): Boolean {
        return if (settings[CustomerSettingsDefinition] in listOf(CustomerType.RETURNING, CustomerType.NEW)) {
            settings[CustomerSessionSettingsDefinition] == true
        } else {
            true
        }
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
