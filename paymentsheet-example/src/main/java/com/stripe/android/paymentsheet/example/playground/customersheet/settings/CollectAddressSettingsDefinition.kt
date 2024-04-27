package com.stripe.android.paymentsheet.example.playground.customersheet.settings

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.example.playground.customersheet.CustomerSheetPlaygroundState
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode as CollectionMode

internal object CollectAddressSettingsDefinition :
    CustomerSheetPlaygroundSettingDefinition<CollectionMode>,
    CustomerSheetPlaygroundSettingDefinition.Saveable<CollectionMode>,
    CustomerSheetPlaygroundSettingDefinition.Displayable<CollectionMode> {

    override val defaultValue: CollectionMode = CollectionMode.Automatic
    override val key: String = "collectAddress"
    override val displayName: String = "Collect Address"
    override val options: List<CustomerSheetPlaygroundSettingDefinition.Displayable.Option<CollectionMode>> by lazy {
        listOf(
            option("Auto", CollectionMode.Automatic),
            option("Never", CollectionMode.Never),
            option("Full", CollectionMode.Full),
        )
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    override fun configure(
        value: CollectionMode,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: CustomerSheetPlaygroundState,
        configurationData: CustomerSheetPlaygroundSettingDefinition.CustomerSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { copy(address = value) }
    }

    override fun convertToString(value: CollectionMode): String = when (value) {
        CollectionMode.Automatic -> "auto"
        CollectionMode.Never -> "never"
        CollectionMode.Full -> "full"
    }

    override fun convertToValue(value: String): CollectionMode = when (value) {
        "auto" -> CollectionMode.Automatic
        "never" -> CollectionMode.Never
        "full" -> CollectionMode.Full
        else -> CollectionMode.Automatic
    }
}
