package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.utils.FeatureFlags

internal object LpmFoundationsSettingDefinition : BooleanSettingsDefinition(
    key = "useLpmFoundations",
    displayName = "Use LPM Foundations",
    defaultValue = false,
) {
    override fun configure(
        value: Boolean,
    ) {
        FeatureFlags.useLpmFoundations.setEnabled(value)
    }
}
