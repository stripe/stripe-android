package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.AddressAutocompletePreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

@OptIn(AddressAutocompletePreview::class)
internal object AutocompleteAddressSettingsDefinition : BooleanSettingsDefinition(
    key = "allowsAutocompleteAddress",
    displayName = "Autocomplete for addresses",
    defaultValue = false,
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.PaymentSheet ||
            configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.FlowController ||
            configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.FlowControllerWithSpt
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
        settings: Settings,
    ) {
        if (value) {
            settings.googlePlacesApiKey?.let {
                configurationBuilder.googlePlacesApiKey(it)
            }
        }
    }
}
