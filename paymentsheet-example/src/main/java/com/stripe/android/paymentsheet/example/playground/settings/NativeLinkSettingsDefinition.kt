package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.link.NativeLinkEnabled
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object NativeLinkSettingsDefinition : BooleanSettingsDefinition(
    key = "nativeLink",
    displayName = "Native Link",
    defaultValue = false,
) {
    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        super.configure(value, configurationBuilder, playgroundState, configurationData)
        NativeLinkEnabled.enabled = value
    }
}
