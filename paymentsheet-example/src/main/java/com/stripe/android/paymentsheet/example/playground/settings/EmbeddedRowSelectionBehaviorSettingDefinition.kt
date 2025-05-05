package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.settings.EmbeddedFormSheetActionSettingDefinition.FormSheetAction

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal object EmbeddedRowSelectionBehaviorSettingDefinition :
    PlaygroundSettingDefinition<EmbeddedRowSelectionBehaviorSettingDefinition.RowSelectionBehavior>,
    PlaygroundSettingDefinition.Saveable<EmbeddedRowSelectionBehaviorSettingDefinition.RowSelectionBehavior> by EnumSaveable(
        key = "embeddedRowSelectionBehavior",
        values = RowSelectionBehavior.entries.toTypedArray(),
        defaultValue = RowSelectionBehavior.Default
    ),
    PlaygroundSettingDefinition.Displayable<EmbeddedRowSelectionBehaviorSettingDefinition.RowSelectionBehavior> {

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.Embedded
    }

    override val displayName: String = "Embedded Row Selection Behavior"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        EmbeddedRowSelectionBehaviorSettingDefinition.option("Default", RowSelectionBehavior.Default),
        EmbeddedRowSelectionBehaviorSettingDefinition.option("Immediate", RowSelectionBehavior.Immediate),
    )

    override fun configure(
        value: RowSelectionBehavior,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationBuilder.rowSelectionBehavior(value.rowSelectionBehavior)
    }


    enum class RowSelectionBehavior(
        override val value: String,
        val rowSelectionBehavior: EmbeddedPaymentElement.RowSelectionBehavior,
    ) : ValueEnum {
        Default("default", EmbeddedPaymentElement.RowSelectionBehavior.Default),
        Immediate("immediate", EmbeddedPaymentElement.RowSelectionBehavior.ImmediateAction)
    }
}