package com.stripe.android.paymentsheet.example.playground.settings

import android.os.Parcelable
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceStore
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

internal object SectionSpacingSettingsDefinition :
    PlaygroundSettingDefinition<SectionSpacing>,
    PlaygroundSettingDefinition.Saveable<SectionSpacing> {
    override val key: String = "sectionSpacing"

    override val defaultValue: SectionSpacing = SectionSpacing.Default

    override fun convertToValue(value: String): SectionSpacing {
        return value.toFloatOrNull()?.let {
            SectionSpacing.Custom(it)
        } ?: SectionSpacing.Default
    }

    override fun convertToString(value: SectionSpacing): String {
        return when (value) {
            is SectionSpacing.Default -> "default"
            is SectionSpacing.Custom -> value.spacingDp.toString()
        }
    }

    @OptIn(AppearanceAPIAdditionsPreview::class)
    override fun setValue(value: SectionSpacing) {
        super.setValue(value)
        AppearanceStore.state = AppearanceStore.state.copy(
            sectionSpacing = when (value) {
                is SectionSpacing.Default -> PaymentSheet.Spacing(-1f)
                is SectionSpacing.Custom -> PaymentSheet.Spacing(value.spacingDp)
            }
        )
    }
}

internal sealed interface SectionSpacing : Parcelable {
    @Serializable
    @Parcelize
    data object Default : SectionSpacing

    @Serializable
    @Parcelize
    data class Custom(
        val spacingDp: Float = 8f,
    ) : SectionSpacing
}
