package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags

internal object PassiveCaptchaDefinition : FeatureFlagSettingsDefinition(
    FeatureFlags.enablePassiveCaptcha,
    allowedIntegrationTypes = PlaygroundConfigurationData.IntegrationType.paymentFlows().toList(),
)
