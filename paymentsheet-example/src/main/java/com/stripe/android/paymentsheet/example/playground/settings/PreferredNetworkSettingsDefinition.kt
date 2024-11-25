package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object PreferredNetworkSettingsDefinition : BooleanSettingsDefinition(
    key = "cartesBancairesAsMerchantPreferredNetwork",
    displayName = "Cartes Bancaires as preferred network",
    defaultValue = false,
) {
    private val preferredNetworks = listOf(CardBrand.CartesBancaires)

    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        if (value) {
            configurationBuilder.preferredNetworks(preferredNetworks)
        }
    }

    @ExperimentalEmbeddedPaymentElementApi
    override fun configure(
        value: Boolean,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        if (value) {
            configurationBuilder.preferredNetworks(preferredNetworks)
        }
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Customer,
        configurationData: PlaygroundSettingDefinition.CustomerSheetConfigurationData,
    ) {
        if (value) {
            configurationBuilder.preferredNetworks(preferredNetworks)
        }
    }
}
