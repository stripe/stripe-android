package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.elements.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.elements.customersheet.CustomerSheet
import com.stripe.android.elements.payment.EmbeddedPaymentElement
import com.stripe.android.elements.payment.FlowController
import com.stripe.android.elements.payment.PaymentSheet
import com.stripe.android.link.LinkController
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object CollectEmailSettingsDefinition : CollectionModeSettingsDefinition(
    key = "collectEmail",
    displayName = "Collect Email",
) {
    override fun configure(
        value: CollectionMode,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { email = value }
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: FlowController.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.FlowControllerConfigurationData,
    ) {
        configurationData.updateBillingDetails { email = value }
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationData.updateBillingDetails { email = value }
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Customer,
        configurationData: PlaygroundSettingDefinition.CustomerSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { email = value }
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { email = value }
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: FlowController.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PlaygroundSettingDefinition.FlowControllerConfigurationData,
    ) {
        configurationData.updateBillingDetails { email = value }
    }

    override fun configure(
        value: CollectionMode,
        configurationBuilder: LinkController.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.LinkControllerConfigurationData,
    ) {
        configurationData.updateBillingDetails { email = value }
    }
}
