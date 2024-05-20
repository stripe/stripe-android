package com.stripe.android.paymentsheet.example.playground.settings

internal abstract class BooleanSettingsDefinition(
    override val key: String,
    override val displayName: String,
    override val defaultValue: Boolean,
) : PlaygroundSettingDefinition<Boolean>,
    PlaygroundSettingDefinition.Saveable<Boolean>,
    PlaygroundSettingDefinition.Displayable<Boolean> {

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        option("On", true),
        option("Off", false),
    )

    override fun convertToString(value: Boolean): String {
        return value.toString()
    }

    override fun convertToValue(value: String): Boolean {
        return value == "true"
    }
}
