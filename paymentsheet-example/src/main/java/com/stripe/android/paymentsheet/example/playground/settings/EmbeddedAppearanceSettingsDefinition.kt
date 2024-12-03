package com.stripe.android.paymentsheet.example.playground.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

internal object RowStyleSettingsDefinition :
    PlaygroundSettingDefinition<RowStyleEnum>,
    PlaygroundSettingDefinition.Saveable<RowStyleEnum> by EnumSaveable(
        key = "RowStyle",
        values = RowStyleEnum.entries.toTypedArray(),
        defaultValue = RowStyleEnum.FlatWithRadio
    )

internal object SeparatorThicknessSettingsDefinition :
    PlaygroundSettingDefinition<Float>, PlaygroundSettingDefinition.Saveable<Float> {
    override val key: String
        get() = "separatorThickness"

    override val defaultValue: Float
        get() = 1.0f

    override fun convertToValue(value: String): Float = value.toFloat()

    override fun convertToString(value: Float): String = value.toString()
}

internal object SeparatorInsetsSettingsDefinition :
    PlaygroundSettingDefinition<Float>,
    PlaygroundSettingDefinition.Saveable<Float> {
    override val key: String
        get() = "separatorInsets"

    override val defaultValue: Float
        get() = 0.0f

    override fun convertToValue(value: String): Float = value.toFloat()

    override fun convertToString(value: Float): String = value.toString()
}

internal object AdditionalInsetsSettingsDefinition :
    PlaygroundSettingDefinition<Float>,
    PlaygroundSettingDefinition.Saveable<Float> {
    override val key: String
        get() = "additionalInsets"

    override val defaultValue: Float
        get() = 4.0f

    override fun convertToValue(value: String): Float = value.toFloat()

    override fun convertToString(value: Float): String = value.toString()
}

internal object CheckmarkInsetsSettingsDefinition :
    PlaygroundSettingDefinition<Float>,
    PlaygroundSettingDefinition.Saveable<Float> {
    override val key: String
        get() = "checkmarkInsets"

    override val defaultValue: Float
        get() = 12.0f

    override fun convertToValue(value: String): Float = value.toFloat()

    override fun convertToString(value: Float): String = value.toString()
}

internal object FloatingButtonSpacingSettingsDefinition :
    PlaygroundSettingDefinition<Float>,
    PlaygroundSettingDefinition.Saveable<Float> {
    override val key: String
        get() = "floatingButtonSpacing"

    override val defaultValue: Float
        get() = 12.0f

    override fun convertToValue(value: String): Float = value.toFloat()

    override fun convertToString(value: Float): String = value.toString()
}

internal object TopSeparatorEnabledSettingsDefinition :
    PlaygroundSettingDefinition<Boolean>,
    PlaygroundSettingDefinition.Saveable<Boolean> {

    override val key: String
        get() = "topSeparatorEnabled"

    override val defaultValue: Boolean = true

    override fun convertToValue(value: String): Boolean = value == "true"

    override fun convertToString(value: Boolean): String = value.toString()
}

internal object BottomSeparatorEnabledSettingsDefinition :
    PlaygroundSettingDefinition<Boolean>,
    PlaygroundSettingDefinition.Saveable<Boolean> {

    override val key: String
        get() = "bottomSeparatorEnabled"

    override val defaultValue: Boolean = true

    override fun convertToValue(value: String): Boolean = value == "true"

    override fun convertToString(value: Boolean): String = value.toString()
}

internal object SeparatorColorSettingsDefinition :
    PlaygroundSettingDefinition<Int>,
    PlaygroundSettingDefinition.Saveable<Int> {
    override val key: String
        get() = "separatorColor"

    override val defaultValue: Int
        get() = Color(0xFF787880).toArgb()

    override fun convertToString(value: Int): String = value.toString()

    override fun convertToValue(value: String): Int = value.toInt()
}

internal object SelectedColorSettingsDefinition :
    PlaygroundSettingDefinition<Int>,
    PlaygroundSettingDefinition.Saveable<Int> {
    override val key: String
        get() = "selectedColor"

    override val defaultValue: Int
        get() = Color(0xFF007AFF).toArgb()

    override fun convertToString(value: Int): String = value.toString()

    override fun convertToValue(value: String): Int = value.toInt()
}

internal object UnselectedColorSettingsDefinition :
    PlaygroundSettingDefinition<Int>,
    PlaygroundSettingDefinition.Saveable<Int> {
    override val key: String
        get() = "unselectedColor"

    override val defaultValue: Int
        get() = Color(0x33787880).toArgb()

    override fun convertToString(value: Int): String = value.toString()

    override fun convertToValue(value: String): Int = value.toInt()
}

internal object CheckmarkColorSettingsDefinition :
    PlaygroundSettingDefinition<Int>,
    PlaygroundSettingDefinition.Saveable<Int> {
    override val key: String
        get() = "checkmarkColor"

    override val defaultValue: Int
        get() = Color(0xFF007AFF).toArgb()

    override fun convertToString(value: Int): String = value.toString()

    override fun convertToValue(value: String): Int = value.toInt()
}

enum class RowStyleEnum(override val value: String) : ValueEnum {
    FlatWithRadio("FlatWithRadio"),
    FlatWithCheckmark("FlatWithCheckmark"),
    FloatingButton("FloatingButton")
}
