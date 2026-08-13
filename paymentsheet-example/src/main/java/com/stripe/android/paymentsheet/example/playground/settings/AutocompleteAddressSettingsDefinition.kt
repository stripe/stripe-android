package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.AddressAutocompletePreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

@OptIn(AddressAutocompletePreview::class)
internal object AutocompleteAddressSettingsDefinition : BooleanSettingsDefinition(
    key = "allowsAutocompleteAddress",
    displayName = "Autocomplete for addresses",
    defaultValue = false,
) {
    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.PaymentSheet ||
            configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.FlowController ||
            configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.FlowControllerWithSpt
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        // Address autocomplete is now powered by Stripe's hosted endpoint — no API key needed.
    }
}
