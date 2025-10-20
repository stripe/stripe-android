package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags

internal object EnablePromptPaySettingsDefinition : FeatureFlagSettingsDefinition(
    FeatureFlags.enablePromptPay,
    allowedIntegrationTypes = PlaygroundConfigurationData.IntegrationType.paymentFlows().toList(),
)
