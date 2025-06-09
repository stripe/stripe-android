package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceStore
import kotlinx.serialization.json.Json

internal object TextFieldInsetsAppearanceSettingDefinition :
    PlaygroundSettingDefinition<FormInsetsAppearance>,
    PlaygroundSettingDefinition.Saveable<FormInsetsAppearance> {
    override val key: String
        get() = "textFieldInsetsAppearance"

    override val defaultValue: FormInsetsAppearance
        get() = FormInsetsAppearance(start = 0f, end = 0f, top = 0f, bottom = 0f)

    override fun convertToValue(value: String): FormInsetsAppearance {
        return Json.decodeFromString<FormInsetsAppearance>(value)
    }

    override fun convertToString(value: FormInsetsAppearance): String {
        return Json.encodeToString(value)
    }

    override fun setValue(value: FormInsetsAppearance) {
        super.setValue(value)
        AppearanceStore.state = AppearanceStore.state.copy(
            textFieldInsets = PaymentSheet.Insets(
                startDp = value.start,
                topDp = value.top,
                endDp = value.end,
                bottomDp = value.bottom,
            )
        )
    }
}
