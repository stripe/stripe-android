package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CaptureMethodSettingsDefinition : BooleanSettingsDefinition(
    defaultValue = false,
    displayName = "Manual Capture",
    key = "useManualCapture"
) {
    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.useManualCapture(value)
    }
}
