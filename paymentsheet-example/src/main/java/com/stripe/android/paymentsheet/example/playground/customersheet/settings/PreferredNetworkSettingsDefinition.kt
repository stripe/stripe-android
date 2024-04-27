package com.stripe.android.paymentsheet.example.playground.customersheet.settings

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.example.playground.customersheet.CustomerSheetPlaygroundState

internal object PreferredNetworkSettingsDefinition : BooleanSettingsDefinition(
    key = "cartesBancairesAsMerchantPreferredNetwork",
    displayName = "Cartes Bancaires as preferred network",
    defaultValue = false,
) {

    @OptIn(ExperimentalCustomerSheetApi::class)
    override fun configure(
        value: Boolean,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: CustomerSheetPlaygroundState,
        configurationData: CustomerSheetPlaygroundSettingDefinition.CustomerSheetConfigurationData
    ) {
        if (value) {
            configurationBuilder.preferredNetworks(listOf(CardBrand.CartesBancaires))
        }
    }
}
