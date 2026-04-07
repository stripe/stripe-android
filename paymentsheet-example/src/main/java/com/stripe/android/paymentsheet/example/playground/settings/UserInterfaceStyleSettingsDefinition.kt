package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object UserInterfaceStyleSettingsDefinition :
    PlaygroundSettingDefinition<StyleOption>,
    PlaygroundSettingDefinition.Saveable<StyleOption> by EnumSaveable(
        key = "colorScheme",
        values = StyleOption.entries.toTypedArray(),
        defaultValue = StyleOption.AUTOMATIC,
    ),
    PlaygroundSettingDefinition.Displayable<StyleOption> {
    override val displayName: String = "User Interface Style"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<StyleOption>> {
        return StyleOption.entries.map { option ->
            option(option.value, option)
        }
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
        value: StyleOption,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configurationBuilder.style(value.style)
    }
}

internal enum class StyleOption(
    override val value: String,
    val style: PaymentSheet.UserInterfaceStyle,
) : ValueEnum {
    AUTOMATIC("Automatic", PaymentSheet.UserInterfaceStyle.Automatic),
    ALWAYS_LIGHT("Always Light", PaymentSheet.UserInterfaceStyle.AlwaysLight),
    ALWAYS_DARK("Always Dark", PaymentSheet.UserInterfaceStyle.AlwaysDark),
}
