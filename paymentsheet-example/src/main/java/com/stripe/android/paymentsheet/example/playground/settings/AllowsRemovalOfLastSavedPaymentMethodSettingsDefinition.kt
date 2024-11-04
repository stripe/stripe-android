package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object AllowsRemovalOfLastSavedPaymentMethodSettingsDefinition : BooleanSettingsDefinition(
    key = "allowsRemovalOfLastSavedPaymentMethod",
    displayName = "Allows removal of last saved payment method",
    defaultValue = true,
) {
    @OptIn(ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi::class)
    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configurationBuilder.allowsRemovalOfLastSavedPaymentMethod(value)
    }

    @ExperimentalEmbeddedPaymentElementApi
    @OptIn(ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi::class)
    override fun configure(
        value: Boolean,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationBuilder.allowsRemovalOfLastSavedPaymentMethod(value)
    }

    @OptIn(ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi::class)
    override fun configure(
        value: Boolean,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Customer,
        configurationData: PlaygroundSettingDefinition.CustomerSheetConfigurationData,
    ) {
        configurationBuilder.allowsRemovalOfLastSavedPaymentMethod(value)
    }
}
