package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlag

internal class FeatureFlagSettingsDefinition(
    private val featureFlag: FeatureFlag,
) : BooleanSettingsDefinition(
    key = "featureFlag_${featureFlag.name}",
    displayName = featureFlag.name,
    defaultValue = false,
) {
    override fun setValue(value: Boolean) {
        super.setValue(value)
        featureFlag.setEnabled(value)
    }
}
