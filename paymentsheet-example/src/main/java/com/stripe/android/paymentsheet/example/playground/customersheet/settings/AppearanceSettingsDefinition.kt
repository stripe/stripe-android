package com.stripe.android.paymentsheet.example.playground.customersheet.settings

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceStore
import com.stripe.android.paymentsheet.example.playground.customersheet.CustomerSheetPlaygroundState

internal object AppearanceSettingsDefinition : CustomerSheetPlaygroundSettingDefinition<Unit> {
    override val defaultValue: Unit = Unit

    @OptIn(ExperimentalCustomerSheetApi::class)
    override fun configure(
        value: Unit,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: CustomerSheetPlaygroundState,
        configurationData: CustomerSheetPlaygroundSettingDefinition.CustomerSheetConfigurationData
    ) {
        configurationBuilder.appearance(AppearanceStore.state)
    }
}
