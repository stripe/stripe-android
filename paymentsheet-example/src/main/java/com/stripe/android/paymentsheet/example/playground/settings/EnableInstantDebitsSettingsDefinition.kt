package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags

internal object EnableInstantDebitsSettingsDefinition : BooleanSettingsDefinition(
    key = "enableInstantDebits",
    displayName = "Enable Instant Debits",
    defaultValue = false,
) {
    override fun valueUpdated(value: Boolean, playgroundSettings: PlaygroundSettings) {
        FeatureFlags.instantDebits.setEnabled(value)
    }
}
