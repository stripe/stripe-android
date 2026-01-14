package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object GooglePayCustomerSheetSettingsDefinition : BooleanSettingsDefinition(
    key = "googlePay",
    displayName = "Google Pay",
    defaultValue = true,
) {
    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>
    ): Boolean {
        return configurationData.integrationType.isCustomerFlow()
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Customer,
        configurationData: PlaygroundSettingDefinition.CustomerSheetConfigurationData
    ) {
        configurationBuilder.googlePayEnabled(value)
    }
}
