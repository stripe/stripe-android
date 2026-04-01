package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags

internal object CardArtSettingsDefinition : FeatureFlagSettingsDefinition(
    featureFlag = FeatureFlags.enableCardArt,
    allowedIntegrationTypes = PlaygroundConfigurationData.IntegrationType.paymentFlows().toList()
)
