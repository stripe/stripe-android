package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceStore

internal object LayoutSettingsDefinition : BooleanSettingsDefinition(
    key = "layout",
    displayName = "Vertical Mode",
    defaultValue = false,
) {
    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configurationBuilder.appearance(
            AppearanceStore.state.copy(
                layout = if (value) {
                    PaymentSheet.Appearance.Layout.Vertical
                } else {
                    PaymentSheet.Appearance.Layout.Horizontal
                }
            )
        )
    }
}
