package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags

internal object NativeLinkSettingsDefinition : BooleanSettingsDefinition(
    key = "nativeLink",
    displayName = "Native Link",
    defaultValue = false,
) {
    override fun setValue(value: Boolean) {
        super.setValue(value)
        FeatureFlags.nativeLinkEnabled.setEnabled(value)
    }
}
