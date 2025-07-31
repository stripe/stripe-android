package com.stripe.android.paymentsheet.example.playground.settings

import android.util.Log
import com.stripe.android.elements.payment.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object EmbeddedRowSelectionBehaviorSettingsDefinition :
    PlaygroundSettingDefinition<
        EmbeddedRowSelectionBehaviorSettingsDefinition.RowSelectionBehavior
        >,
    PlaygroundSettingDefinition.Saveable<EmbeddedRowSelectionBehaviorSettingsDefinition.RowSelectionBehavior> by
    EnumSaveable(
        key = "embeddedRowSelectionBehavior",
        values = RowSelectionBehavior.entries.toTypedArray(),
        defaultValue = RowSelectionBehavior.Default
    ),
    PlaygroundSettingDefinition.Displayable<EmbeddedRowSelectionBehaviorSettingsDefinition.RowSelectionBehavior> {

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.Embedded
    }

    override val displayName: String = "Embedded Row Selection Behavior"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        option("Default", RowSelectionBehavior.Default),
        option("Immediate Action", RowSelectionBehavior.ImmediateAction),
    )

    override fun configure(
        value: RowSelectionBehavior,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        // This has to run in the EmbeddedPaymentElement Builder
        return
    }

    enum class RowSelectionBehavior(
        override val value: String,
        val rowSelectionBehavior: EmbeddedPaymentElement.RowSelectionBehavior,
    ) : ValueEnum {
        Default("default", EmbeddedPaymentElement.RowSelectionBehavior.default()),
        ImmediateAction(
            "immediate_action",
            EmbeddedPaymentElement.RowSelectionBehavior.immediateAction { embeddedPaymentElement ->
                val paymentOption = embeddedPaymentElement.paymentOption.value
                Log.d(
                    "ImmediateAction",
                    "Payment Option ${paymentOption?.paymentMethodType}: ${paymentOption?.label}"
                )
            }
        )
    }
}
