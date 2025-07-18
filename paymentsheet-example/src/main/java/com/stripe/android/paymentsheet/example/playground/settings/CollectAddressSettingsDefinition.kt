package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.link.LinkController
import com.stripe.android.paymentelement.EmbeddedPaymentElement
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

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        option("Auto", CollectionMode.Automatic),
        option("Never", CollectionMode.Never),
        option("Full", CollectionMode.Full),
    )

    override fun configure(
        value: CollectionMode,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { address = value }
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationData.updateBillingDetails { address = value }
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Customer,
        configurationData: PlaygroundSettingDefinition.CustomerSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { address = value }
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { address = value }
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: LinkController.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.LinkControllerConfigurationData,
    ) {
        configurationData.updateBillingDetails { address = value }
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
