package com.stripe.android.paymentsheet.example.playground.settings

import android.os.Parcelable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceStore
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object FormInsetsAppearanceSettingDefinition :
    PlaygroundSettingDefinition<FormInsetsAppearance>,
    PlaygroundSettingDefinition.Saveable<FormInsetsAppearance> {
    override val key: String
        get() = "formInsetsAppearance"

    override val defaultValue: FormInsetsAppearance
        get() = FormInsetsAppearance()

    override fun convertToValue(value: String): FormInsetsAppearance {
        return Json.decodeFromString<FormInsetsAppearance>(value)
    }

    override fun convertToString(value: FormInsetsAppearance): String {
        return Json.encodeToString(value)
    }

    override fun setValue(value: FormInsetsAppearance) {
        super.setValue(value)
        AppearanceStore.state = AppearanceStore.state.copy(
            formInsetValues = PaymentSheet.Insets(
                startDp = value.start,
                topDp = value.top,
                endDp = value.end,
                bottomDp = value.bottom,
            )
        )
    }
}

@Serializable
@Parcelize
internal data class FormInsetsAppearance(
    val start: Float = 20f,
    val top: Float = 0f,
    val end: Float = 20f,
    val bottom: Float = 40f
) : Parcelable
