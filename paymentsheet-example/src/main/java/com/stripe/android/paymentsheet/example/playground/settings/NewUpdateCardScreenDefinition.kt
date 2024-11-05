package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags

internal object NewUpdateCardScreenDefinition : BooleanSettingsDefinition(
    key = "new_update_card_screen",
    displayName = "Enable new update card screen",
    defaultValue = false,
) {
    override fun valueUpdated(value: Boolean, playgroundSettings: PlaygroundSettings) {
        super.valueUpdated(value, playgroundSettings)
        FeatureFlags.useNewUpdateCardScreen.setEnabled(isEnabled = value)
    }
}
