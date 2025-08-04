package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.elements.customersheet.CustomerSheet
import com.stripe.android.elements.payment.EmbeddedPaymentElement
import com.stripe.android.elements.payment.FlowController
import com.stripe.android.elements.payment.PaymentSheet
import com.stripe.android.link.LinkController
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object AttachBillingDetailsToPaymentMethodSettingsDefinition : BooleanSettingsDefinition(
    key = "attachDefaults",
    displayName = "Attach Billing Details to Payment Method",
    defaultValue = true,
) {
    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { attachDefaultsToPaymentMethod = value }
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: FlowController.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        flowControllerConfigurationData: PlaygroundSettingDefinition.FlowControllerConfigurationData,
    ) {
        flowControllerConfigurationData.updateBillingDetails { attachDefaultsToPaymentMethod = value }
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationData.updateBillingDetails { attachDefaultsToPaymentMethod = value }
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Customer,
        configurationData: PlaygroundSettingDefinition.CustomerSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { attachDefaultsToPaymentMethod = value }
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { attachDefaultsToPaymentMethod = value }
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: FlowController.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        flowControllerConfigurationData: PlaygroundSettingDefinition.FlowControllerConfigurationData,
    ) {
        flowControllerConfigurationData.updateBillingDetails { attachDefaultsToPaymentMethod = value }
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: LinkController.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.LinkControllerConfigurationData,
    ) {
        configurationData.updateBillingDetails { attachDefaultsToPaymentMethod = value }
    }
}
