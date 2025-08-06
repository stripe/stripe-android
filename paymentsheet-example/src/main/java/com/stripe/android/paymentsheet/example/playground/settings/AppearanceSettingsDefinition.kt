package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.elements.customersheet.CustomerSheet
import com.stripe.android.elements.payment.EmbeddedPaymentElement
import com.stripe.android.elements.payment.FlowController
import com.stripe.android.elements.payment.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceStore

internal object AppearanceSettingsDefinition : PlaygroundSettingDefinition<Unit> {
    override val defaultValue: Unit = Unit

    override fun configure(
        value: Unit,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configurationBuilder.appearance(AppearanceStore.state.toPaymentSheetAppearance())
    }

    override fun configure(
        value: Unit,
        configurationBuilder: FlowController.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.FlowControllerConfigurationData
    ) {
        configurationBuilder.appearance(AppearanceStore.state.toPaymentSheetAppearance())
    }

    override fun configure(
        value: Unit,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationBuilder.appearance(AppearanceStore.state.toPaymentSheetAppearance())
    }

    override fun configure(
        value: Unit,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Customer,
        configurationData: PlaygroundSettingDefinition.CustomerSheetConfigurationData,
    ) {
        configurationBuilder.appearance(AppearanceStore.state.toPaymentSheetAppearance())
    }

    override fun configure(
        value: Unit,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationBuilder.appearance(AppearanceStore.state.toPaymentSheetAppearance())
    }

    override fun configure(
        value: Unit,
        configurationBuilder: FlowController.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PlaygroundSettingDefinition.FlowControllerConfigurationData,
    ) {
        configurationBuilder.appearance(AppearanceStore.state.toPaymentSheetAppearance())
    }
}
