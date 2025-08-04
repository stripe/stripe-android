package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.elements.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.elements.customersheet.CustomerSheet
import com.stripe.android.elements.payment.EmbeddedPaymentElement
import com.stripe.android.elements.payment.FlowController
import com.stripe.android.elements.payment.PaymentSheet
import com.stripe.android.link.LinkController
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object CollectNameSettingsDefinition : CollectionModeSettingsDefinition(
    key = "collectName",
    displayName = "Collect Name",
) {
    override fun configure(
        value: CollectionMode,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { name = value }
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: FlowController.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.FlowControllerConfigurationData,
    ) {
        configurationData.updateBillingDetails { name = value }
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationData.updateBillingDetails { name = value }
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Customer,
        configurationData: PlaygroundSettingDefinition.CustomerSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { name = value }
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { name = value }
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: FlowController.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PlaygroundSettingDefinition.FlowControllerConfigurationData,
    ) {
        configurationData.updateBillingDetails { name = value }
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: LinkController.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.LinkControllerConfigurationData,
    ) {
        configurationData.updateBillingDetails { name = value }
    }
}
