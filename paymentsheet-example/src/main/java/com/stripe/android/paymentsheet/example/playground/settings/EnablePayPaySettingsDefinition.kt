package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags

internal object EnablePayPaySettingsDefinition : FeatureFlagSettingsDefinition(
    FeatureFlags.enablePayPay,
    allowedIntegrationTypes = PlaygroundConfigurationData.IntegrationType.paymentFlows().toList(),
)
