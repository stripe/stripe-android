package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object NewUpdateCardScreenDefinition : BooleanSettingsDefinition(
    key = "new_update_card_screen",
    displayName = "Enable new update card screen",
    defaultValue = false,
) {
    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        super.configure(value, checkoutRequestBuilder)
        FeatureFlags.useNewUpdateCardScreen.setEnabled(isEnabled = value)
    }
}
