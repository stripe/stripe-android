package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object EnableInstantDebitsSettingsDefinition : BooleanSettingsDefinition(
    key = "enableInstantDebits",
    displayName = "Enable Instant Debits",
    defaultValue = false,
) {

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        // Hijacking this method to synchronize the setting with the feature flag
        FeatureFlags.instantDebits.setEnabled(value)
    }

    override fun valueUpdated(value: Boolean, playgroundSettings: PlaygroundSettings) {
        FeatureFlags.instantDebits.setEnabled(value)
    }
}
