package com.stripe.android.paymentsheet.example.playground.settings

internal object CvcRecollectionEnabledCallbackValue : BooleanSettingsDefinition(
    key = "cvcRecollectionEnabledCallbackValue",
    displayName = "CVC Recollection Enabled Callback Value",
    defaultValue = false
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }
}
