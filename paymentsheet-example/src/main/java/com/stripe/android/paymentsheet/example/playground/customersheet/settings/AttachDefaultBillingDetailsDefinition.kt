package com.stripe.android.paymentsheet.example.playground.customersheet.settings

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.example.playground.customersheet.CustomerSheetPlaygroundState

internal object AttachDefaultBillingDetailsDefinition : BooleanSettingsDefinition(
    key = "attachDefaults",
    displayName = "Attach Billing Details to Payment Method",
    defaultValue = true,
) {
    @OptIn(ExperimentalCustomerSheetApi::class)
    override fun configure(
        value: Boolean,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: CustomerSheetPlaygroundState,
        configurationData: CustomerSheetPlaygroundSettingDefinition.CustomerSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { copy(attachDefaultsToPaymentMethod = value) }
    }
}
