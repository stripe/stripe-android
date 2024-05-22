package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object CollectNameSettingsDefinition : CollectionModeSettingsDefinition(
    key = "collectName",
    displayName = "Collect Name",
) {
    override fun configure(
        value: CollectionMode,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { copy(name = value) }
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    override fun configure(
        value: CollectionMode,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PlaygroundSettingDefinition.CustomerSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { copy(name = value) }
    }
}
