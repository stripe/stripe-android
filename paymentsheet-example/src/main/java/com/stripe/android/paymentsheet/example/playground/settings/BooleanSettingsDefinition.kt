package com.stripe.android.paymentsheet.example.playground.settings

internal abstract class BooleanSettingsDefinition(
    key: String,
    displayName: String,
    override val defaultValue: Boolean,
) : PlaygroundSettingDefinition<Boolean>(key, displayName) {
    override val options: List<Option<Boolean>> = listOf(
        Option("On", true),
        Option("Off", false),
    )

    override fun convertToString(value: Boolean): String {
        return value.toString()
    }

    override fun convertToValue(value: String): Boolean {
        return value == "true"
    }
}
