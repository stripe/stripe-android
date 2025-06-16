package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceStore

@OptIn(AppearanceAPIAdditionsPreview::class)
internal object IconStyleAppearanceSettingsDefinition :
    PlaygroundSettingDefinition<PaymentSheet.IconStyle>,
    PlaygroundSettingDefinition.Saveable<PaymentSheet.IconStyle> {
    override val key: String
        get() = "iconStyleAppearance"

    override val defaultValue: PaymentSheet.IconStyle
        get() = PaymentSheet.IconStyle.Filled

    override fun convertToValue(value: String): PaymentSheet.IconStyle {
        return PaymentSheet.IconStyle.valueOf(value)
    }

    override fun convertToString(value: PaymentSheet.IconStyle): String {
        return value.name
    }

    @OptIn(AppearanceAPIAdditionsPreview::class)
    override fun setValue(value: PaymentSheet.IconStyle) {
        super.setValue(value)

        AppearanceStore.state = AppearanceStore.state.copy(
            iconStyle = value,
        )
    }
}
