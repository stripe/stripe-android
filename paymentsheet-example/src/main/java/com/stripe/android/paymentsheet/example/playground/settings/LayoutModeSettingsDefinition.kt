package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceStore

internal object LayoutModeSettingsDefinition : BooleanSettingsDefinition(
    key = "layoutMode",
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
                layoutMode = if (value) {
                    PaymentSheet.Appearance.LayoutMode.Vertical
                } else {
                    PaymentSheet.Appearance.LayoutMode.Horizontal
                }
            )
        )
    }
}
