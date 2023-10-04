package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode as CollectionMode

internal object CollectAddressSettingsDefinition :
    PlaygroundSettingDefinition<CollectionMode>,
    PlaygroundSettingDefinition.Saveable<CollectionMode>,
    PlaygroundSettingDefinition.Displayable<CollectionMode> {

    override val defaultValue: CollectionMode = CollectionMode.Automatic
    override val key: String = "collectAddress"
    override val displayName: String = "Collect Address"
    override val options: List<PlaygroundSettingDefinition.Displayable.Option<CollectionMode>> by lazy {
        listOf(
            option("Auto", CollectionMode.Automatic),
            option("Never", CollectionMode.Never),
            option("Full", CollectionMode.Full),
        )
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
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
