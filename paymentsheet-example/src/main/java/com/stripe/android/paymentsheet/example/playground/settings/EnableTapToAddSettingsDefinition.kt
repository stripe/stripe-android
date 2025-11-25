package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object EnableTapToAddSettingsDefinition : FeatureFlagSettingsDefinition(
    featureFlag = FeatureFlags.enableTapToAdd,
    allowedIntegrationTypes = PlaygroundConfigurationData.IntegrationType.paymentFlows().toList()
) {
    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.allowsTapToAdd(value)
    }
}
