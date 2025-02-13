package com.stripe.android.paymentsheet.example.playground.settings

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceStore
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

    @OptIn(ExperimentalEmbeddedPaymentElementApi::class)
    override fun valueUpdated(value: EmbeddedAppearance, playgroundSettings: PlaygroundSettings) {
        super.valueUpdated(value, playgroundSettings)
        AppearanceStore.state = AppearanceStore.state.copy(
            embeddedAppearance = PaymentSheet.Appearance.Embedded(value.getRow())
        )
    }
}

internal enum class EmbeddedRow {
    FlatWithRadio,
    FlatWithCheckmark,
    FloatingButton
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Serializable
@Parcelize
internal data class EmbeddedAppearance(
    val embeddedRowStyle: EmbeddedRow = EmbeddedRow.FlatWithRadio,
    val separatorThicknessDp: Float = 1.0f,
    val startSeparatorInset: Float = 0.0f,
    val endSeparatorInset: Float = 0.0f,
    val additionalVerticalInsetsDp: Float = 4.0f,
    val additionalHorizontalInsetsDp: Float = 0.0f,
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
        return when (embeddedRowStyle) {
            EmbeddedRow.FlatWithRadio -> PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio(
                separatorThicknessDp = separatorThicknessDp,
                separatorColor = separatorColor,
                startSeparatorInsetDp = startSeparatorInset,
                endSeparatorInsetDp = endSeparatorInset,
                topSeparatorEnabled = topSeparatorEnabled,
                bottomSeparatorEnabled = bottomSeparatorEnabled,
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                additionalVerticalInsetsDp = additionalVerticalInsetsDp,
                additionalHorizontalInsetsDp = additionalHorizontalInsetsDp
            )
            EmbeddedRow.FlatWithCheckmark -> PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark(
                separatorThicknessDp = separatorThicknessDp,
                separatorColor = separatorColor,
                startSeparatorInsetDp = startSeparatorInset,
                endSeparatorInsetDp = endSeparatorInset,
                topSeparatorEnabled = topSeparatorEnabled,
                bottomSeparatorEnabled = bottomSeparatorEnabled,
                checkmarkColor = checkmarkColor,
                checkmarkInsetDp = checkmarkInsetsDp,
                additionalVerticalInsetsDp = additionalVerticalInsetsDp,
                additionalHorizontalInsetsDp = additionalHorizontalInsetsDp
            )
            EmbeddedRow.FloatingButton -> PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton(
                spacingDp = floatingButtonSpacingDp,
                additionalInsetsDp = additionalVerticalInsetsDp
            )
        }
    }
}
