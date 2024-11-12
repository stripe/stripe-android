package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlag
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal class FeatureFlagSettingsDefinition(
    private val featureFlag: FeatureFlag,
) : BooleanSettingsDefinition(
    key = "featureFlag_${featureFlag.name}",
    displayName = featureFlag.name,
    defaultValue = false,
) {
    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        super.configure(value, configurationBuilder, playgroundState, configurationData)
        featureFlag.setEnabled(value)
    }
}
