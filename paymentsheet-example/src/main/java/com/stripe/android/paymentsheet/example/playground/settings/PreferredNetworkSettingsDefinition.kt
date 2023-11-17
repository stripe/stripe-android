package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object PreferredNetworkSettingsDefinition : BooleanSettingsDefinition(
    key = "cartesBancairesAsMerchantPreferredNetwork",
    displayName = "Cartes Bancaires as preferred network",
    defaultValue = false,
) {

    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        if (value) {
            configurationBuilder.preferredNetworks(listOf(CardBrand.CartesBancaires))
        }
    }
}
