package com.stripe.android.paymentsheet.example.playground.settings

internal abstract class StringSettingDefinition(
    key: String,
    displayName: String,
) : PlaygroundSettingDefinition<String>(key, displayName) {
    override fun convertToString(value: String): String = value
    override fun convertToValue(value: String): String = value
}
