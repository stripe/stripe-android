package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.SavePaymentMethodOptInBehavior
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object SavePaymentMethodOptInBehaviorSettingsDefinition :
    PlaygroundSettingDefinition.Saveable<SavePaymentMethodOptInBehavior>,
    PlaygroundSettingDefinition.Displayable<SavePaymentMethodOptInBehavior> {
    override val key: String = "savePaymentMethodOptInBehavior"
    override val displayName: String = "Save payment method opt-in behavior"
    override val defaultValue: SavePaymentMethodOptInBehavior = SavePaymentMethodOptInBehavior.Automatic

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        option("Automatic", SavePaymentMethodOptInBehavior.Automatic),
        option("Opt-in", SavePaymentMethodOptInBehavior.RequiresOptIn),
        option("Opt-out", SavePaymentMethodOptInBehavior.RequiresOptOut),
    )

    override fun convertToString(value: SavePaymentMethodOptInBehavior): String = value.name

    override fun convertToValue(value: String): SavePaymentMethodOptInBehavior {
        return SavePaymentMethodOptInBehavior.entries.firstOrNull { it.name == value } ?: defaultValue
    }

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return configurationData.integrationType in setOf(
            PlaygroundConfigurationData.IntegrationType.PaymentSheet,
            PlaygroundConfigurationData.IntegrationType.FlowController,
        )
    }

    override fun configure(
        value: SavePaymentMethodOptInBehavior,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configurationBuilder.savePaymentMethodOptInBehavior(value)
    }
}
