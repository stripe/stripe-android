package com.stripe.android.paymentsheet.example.playground.customersheet.settings

internal abstract class BooleanSettingsDefinition(
    override val key: String,
    override val displayName: String,
    override val defaultValue: Boolean,
) : CustomerSheetPlaygroundSettingDefinition<Boolean>,
    CustomerSheetPlaygroundSettingDefinition.Saveable<Boolean>,
    CustomerSheetPlaygroundSettingDefinition.Displayable<Boolean> {
    override val options: List<CustomerSheetPlaygroundSettingDefinition.Displayable.Option<Boolean>> by lazy {
        listOf(
            option("On", true),
            option("Off", false),
        )
    }

    override fun convertToString(value: Boolean): String {
        return value.toString()
    }

    override fun convertToValue(value: String): Boolean {
        return value == "true"
    }
}
