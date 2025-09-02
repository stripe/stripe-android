package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags

internal object EnablePayNowSettingsDefinition : FeatureFlagSettingsDefinition(
    FeatureFlags.enablePayNow,
    allowedIntegrationTypes = PlaygroundConfigurationData.IntegrationType.paymentFlows().toList(),
)
