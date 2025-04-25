package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal object EmbeddedFormSheetActionSettingDefinition :
    PlaygroundSettingDefinition<
        EmbeddedFormSheetActionSettingDefinition.FormSheetAction
        >,
    PlaygroundSettingDefinition.Saveable<EmbeddedFormSheetActionSettingDefinition.FormSheetAction> by EnumSaveable(
        key = "embeddedFormSheetAction",
        values = FormSheetAction.entries.toTypedArray(),
        defaultValue = FormSheetAction.Confirm
    ),
    PlaygroundSettingDefinition.Displayable<EmbeddedFormSheetActionSettingDefinition.FormSheetAction> {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.Embedded
    }

    override val displayName: String = "Embedded Form Sheet Action"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        option("Continue", FormSheetAction.Continue),
        option("Confirm", FormSheetAction.Confirm),
    )

    override fun configure(
        value: FormSheetAction,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationBuilder.formSheetAction(value.formSheetAction)
    }

    enum class FormSheetAction(
        override val value: String,
        val formSheetAction: EmbeddedPaymentElement.FormSheetAction,
    ) : ValueEnum {
        Confirm("confirm", EmbeddedPaymentElement.FormSheetAction.Confirm),
        Continue("continue", EmbeddedPaymentElement.FormSheetAction.Continue)
    }
}
