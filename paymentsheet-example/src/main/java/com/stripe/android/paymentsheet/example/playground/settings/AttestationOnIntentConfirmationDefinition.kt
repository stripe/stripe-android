package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags

internal object AttestationOnIntentConfirmationDefinition : FeatureFlagSettingsDefinition(
    FeatureFlags.enableAttestationOnIntentConfirmation,
    allowedIntegrationTypes = PlaygroundConfigurationData.IntegrationType.paymentFlows().toList(),
)
