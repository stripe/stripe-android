package com.stripe.android.paymentsheet.example.playground.settings

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object EmbeddedAppearanceSettingsDefinition :
    PlaygroundSettingDefinition<EmbeddedAppearance>,
    PlaygroundSettingDefinition.Saveable<EmbeddedAppearance> {
    override val key: String
        get() = "embeddedAppearance"

    override val defaultValue: EmbeddedAppearance
        get() = EmbeddedAppearance()

    override fun convertToValue(value: String): EmbeddedAppearance {
        return Json.decodeFromString<EmbeddedAppearance>(value)
    }

    override fun convertToString(value: EmbeddedAppearance): String {
        return Json.encodeToString(value)
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Serializable
@Parcelize
data class EmbeddedAppearance(
    val rowStyle: String = "flatWithRadio",
    val separatorThicknessDp: Float = 1.0f,
    val separatorInsetsDp: Float = 0.0f,
    val additionalInsetsDp: Float = 4.0f,
    val checkmarkInsetsDp: Float = 12.0f,
    val floatingButtonSpacingDp: Float = 12.0f,
    val topSeparatorEnabled: Boolean = true,
    val bottomSeparatorEnabled: Boolean = true,
    val separatorColor: Int = Color(0xFF787880).toArgb(),
    val selectedColor: Int = Color(0xFF007AFF).toArgb(),
    val unselectedColor: Int = Color(0x33787880).toArgb(),
    val checkmarkColor: Int = Color(0xFF007AFF).toArgb()
) : Parcelable {
    fun getRow(): PaymentSheet.Appearance.Embedded.RowStyle {
        return when (rowStyle) {
            "flatWithRadio" -> PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio(
                separatorThicknessDp = separatorThicknessDp,
                separatorColor = separatorColor,
                separatorInsetsDp = separatorInsetsDp,
                topSeparatorEnabled = topSeparatorEnabled,
                bottomSeparatorEnabled = bottomSeparatorEnabled,
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                additionalInsetsDp = additionalInsetsDp
            )
            "flatWithCheckmark" -> PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark(
                separatorThicknessDp = separatorThicknessDp,
                separatorColor = separatorColor,
                separatorInsetsDp = separatorInsetsDp,
                topSeparatorEnabled = topSeparatorEnabled,
                bottomSeparatorEnabled = bottomSeparatorEnabled,
                checkmarkColor = checkmarkColor,
                checkmarkInsetDp = checkmarkInsetsDp,
                additionalInsetsDp = additionalInsetsDp
            )
            else -> PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton(
                spacingDp = floatingButtonSpacingDp,
                additionalInsetsDp = additionalInsetsDp
            )
        }
    }
}
