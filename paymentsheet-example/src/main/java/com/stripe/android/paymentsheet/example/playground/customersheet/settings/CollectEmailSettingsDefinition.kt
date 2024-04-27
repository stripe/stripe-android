package com.stripe.android.paymentsheet.example.playground.customersheet.settings

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.example.playground.customersheet.CustomerSheetPlaygroundState

internal object CollectEmailSettingsDefinition : CollectionModeSettingsDefinition(
    key = "collectEmail",
    displayName = "Collect Email",
) {
    @OptIn(ExperimentalCustomerSheetApi::class)
    override fun configure(
        value: CollectionMode,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: CustomerSheetPlaygroundState,
        configurationData: CustomerSheetPlaygroundSettingDefinition.CustomerSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { copy(email = value) }
    }
}
