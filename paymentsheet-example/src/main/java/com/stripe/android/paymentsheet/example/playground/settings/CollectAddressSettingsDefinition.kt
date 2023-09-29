package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode as CollectionMode

internal object CollectAddressSettingsDefinition : PlaygroundSettingDefinition<CollectionMode>(
    key = "collectAddress",
    displayName = "Collect Address",
) {
    override val defaultValue: CollectionMode = CollectionMode.Automatic
    override val options: List<Option<CollectionMode>> = listOf(
        Option("Auto", CollectionMode.Automatic),
        Option("Never", CollectionMode.Never),
        Option("Full", CollectionMode.Full),
    )

    override fun configure(
        value: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PaymentSheetConfigurationData,
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
