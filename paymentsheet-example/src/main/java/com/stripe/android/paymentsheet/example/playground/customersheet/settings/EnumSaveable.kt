package com.stripe.android.paymentsheet.example.playground.customersheet.settings

internal class EnumSaveable<T : ValueEnum>(
    override val key: String,
    override val defaultValue: T,
    private val values: Array<T>,
) : CustomerSheetPlaygroundSettingDefinition.Saveable<T> {
    override fun convertToValue(value: String): T {
        return values.firstOrNull { it.value == value } ?: defaultValue
    }

    override fun convertToString(value: T): String {
        return value.value
    }
}

internal interface ValueEnum {
    val value: String
}
